package com.jeromegsq.aynthorautodim

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

class DimmingService : AccessibilityService() {

    private var overlayView: View? = null
    private var targetWindowManager: WindowManager? = null
    private var targetDisplayId: Int = Display.INVALID_DISPLAY
    
    private val handler = Handler(Looper.getMainLooper())
    private val dimRunnable = Runnable { showBlackScreen() }
    
    private var inactivityDelayMs = 3000L
    private var overlayOpacity = 1f
    private var trueBlackMode = false

    private val configReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_UPDATE_CONFIG) {
                Log.d(TAG, "Configuration update received")
                loadPreferences()
                // If the overlay is currently shown, we might want to update it or reset.
                // Simplest approach: reset timer. This will remove overlay (wake up) and restart timer with new settings.
                resetTimer()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service Connected")
        
        loadPreferences()

        val filter = IntentFilter(ACTION_UPDATE_CONFIG)
        if (Build.VERSION.SDK_INT >= 34) {
            registerReceiver(configReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(configReceiver, filter)
        }
        
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                checkForSecondaryDisplay(displayManager)
            }

            override fun onDisplayRemoved(displayId: Int) {
                if (displayId == targetDisplayId) {
                    removeOverlay()
                    targetDisplayId = Display.INVALID_DISPLAY
                    targetWindowManager = null
                }
            }

            override fun onDisplayChanged(displayId: Int) {
                 // Optional: handle changes
            }
        }, handler)

        checkForSecondaryDisplay(displayManager)
        resetTimer()
    }

    private fun checkForSecondaryDisplay(displayManager: DisplayManager) {
        if (targetDisplayId != Display.INVALID_DISPLAY) return

        val displays = displayManager.displays
        for (display in displays) {
            if (display.displayId != Display.DEFAULT_DISPLAY) {
                // Found a secondary display
                targetDisplayId = display.displayId
                Log.d(TAG, "Found secondary display: $targetDisplayId")
                
                val displayContext = createDisplayContext(display)
                targetWindowManager = displayContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                
                resetTimer()
                return
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Ignore events from our own app to prevent feedback loop (Show -> Event -> Reset -> Hide -> Flashing)
        if (event.packageName != null && event.packageName == packageName) {
            return
        }
        
        // Check if event is from the target display
        if (Build.VERSION.SDK_INT >= 30) {
            if (targetDisplayId != Display.INVALID_DISPLAY && event.displayId == targetDisplayId) {
                 resetTimer()
            }
        } else {
            // Fallback for older APIs: we can't distinguish easily, so we reset on any event
            // or we could try to inspect window info if performance allows, but let's keep it simple.
            resetTimer()
        }
    }

    override fun onInterrupt() {
        removeOverlay()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(configReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered or already unregistered
        }
        removeOverlay()
    }

    private fun showBlackScreen() {
        if (overlayView != null) return 
        if (targetWindowManager == null) return 

        Log.d(TAG, "Showing black screen")

        overlayView = View(this).apply {
            setBackgroundColor(Color.BLACK)
            alpha = 0f
            
            if (trueBlackMode) {
                // In True Black mode, we catch the touch to dismiss
                setOnClickListener { 
                    resetTimer()
                }
            }
        }

        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        if (!trueBlackMode) {
             // If NOT true black mode, allow touches to pass through
             // This will cause the system to limit opacity to ~0.8
             flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.OPAQUE
        )
        
        try {
            targetWindowManager?.addView(overlayView, params)
            overlayView?.animate()
                ?.alpha(overlayOpacity)
                ?.setDuration(300)
                ?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error adding view", e)
        }
    }

    private fun removeOverlay() {
        if (overlayView != null && targetWindowManager != null) {
            try {
                overlayView?.animate()?.cancel()
                targetWindowManager?.removeView(overlayView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing view", e)
            }
            overlayView = null
        }
    }

    private fun resetTimer() {
        // If overlay is showing, hide it
        if (overlayView != null) {
            removeOverlay()
        }
        
        // Reload prefs to catch changes on the fly
        loadPreferences()
        
        handler.removeCallbacks(dimRunnable)
        handler.postDelayed(dimRunnable, inactivityDelayMs)
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        inactivityDelayMs = prefs.getLong("inactivity_delay_ms", 3000L)
        overlayOpacity = prefs.getInt("overlay_opacity", 100) / 100f
        trueBlackMode = prefs.getBoolean("true_black_mode", false)
        Log.d(TAG, "Loaded prefs: delay=${inactivityDelayMs}ms, opacity=$overlayOpacity, trueBlack=$trueBlackMode")
    }

    companion object {
        private const val TAG = "DimmingService"
        const val ACTION_UPDATE_CONFIG = "com.jeromegsq.aynthorautodim.UPDATE_CONFIG"
    }
}


package com.jeromegsq.aynthorautodim

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import androidx.core.app.NotificationCompat

class DimmingService : AccessibilityService() {

    private var overlayView: View? = null
    private var targetWindowManager: WindowManager? = null
    private var targetDisplayId: Int = Display.INVALID_DISPLAY
    
    private val handler = Handler(Looper.getMainLooper())
    private val dimRunnable = Runnable { showBlackScreen() }
    
    private var inactivityDelayMs = 3000L
    private var isServicePaused = false

    private val configReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_UPDATE_CONFIG) {
                Log.d(TAG, "Configuration update received")
                loadPreferences()
                if (!isServicePaused) {
                    resetTimer()
                }
            } else if (intent?.action == ACTION_TOGGLE_SERVICE) {
                isServicePaused = !isServicePaused
                Log.d(TAG, "Service toggled: paused=$isServicePaused")
                updateNotification()
                if (isServicePaused) {
                    removeOverlay()
                    handler.removeCallbacks(dimRunnable)
                } else {
                    resetTimer()
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service Connected")
        
        loadPreferences()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        val filter = IntentFilter().apply {
            addAction(ACTION_UPDATE_CONFIG)
            addAction(ACTION_TOGGLE_SERVICE)
        }
        
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
        if (!isServicePaused) {
            resetTimer()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val toggleIntent = Intent(ACTION_TOGGLE_SERVICE).setPackage(packageName)
        val pendingToggleIntent = PendingIntent.getBroadcast(
            this, 0, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val actionText = if (isServicePaused) getString(R.string.action_resume) else getString(R.string.action_pause)
        val contentText = if (isServicePaused) getString(R.string.notification_text_paused) else getString(R.string.notification_text_running)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .addAction(0, actionText, pendingToggleIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification())
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
                
                if (!isServicePaused) {
                    resetTimer()
                }
                return
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (isServicePaused) return

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
        if (isServicePaused) return
        if (overlayView != null) return 
        if (targetWindowManager == null) return 

        Log.d(TAG, "Showing black screen")

        overlayView = View(this).apply {
            setBackgroundColor(Color.BLACK)
            alpha = 0f
            
            // Always catch touch to dismiss (True Black logic)
            setOnClickListener { 
                resetTimer()
            }
        }

        // Always using True Black mode logic:
        // FLAG_NOT_FOCUSABLE allows interaction with system UI if needed (like status bar),
        // but we are NOT setting FLAG_NOT_TOUCHABLE, so we intercept touches.
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        )
        
        try {
            targetWindowManager?.addView(overlayView, params)
            overlayView?.animate()
                ?.alpha(1f)
                ?.setDuration(500)
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
        if (isServicePaused) return

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
        Log.d(TAG, "Loaded prefs: delay=${inactivityDelayMs}ms")
    }

    companion object {
        private const val TAG = "DimmingService"
        const val ACTION_UPDATE_CONFIG = "com.jeromegsq.aynthorautodim.UPDATE_CONFIG"
        const val ACTION_TOGGLE_SERVICE = "com.jeromegsq.aynthorautodim.TOGGLE_SERVICE"
        private const val CHANNEL_ID = "dimming_service_channel"
        private const val NOTIFICATION_ID = 1
    }
}

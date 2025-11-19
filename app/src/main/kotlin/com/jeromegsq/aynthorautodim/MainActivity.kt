package com.jeromegsq.aynthorautodim

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var permissionButton: Button
    private lateinit var serviceButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        permissionButton = findViewById(R.id.permissionButton)
        serviceButton = findViewById(R.id.serviceButton)

        permissionButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        serviceButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val hasOverlayPermission = Settings.canDrawOverlays(this)
        permissionButton.isEnabled = !hasOverlayPermission
        permissionButton.text = if (hasOverlayPermission) "Overlay Permission Granted" else "Grant Overlay Permission"

        val isServiceEnabled = isAccessibilityServiceEnabled()
        serviceButton.text = if (isServiceEnabled) "Service Active (Settings)" else "Enable Service"
        
        if (hasOverlayPermission && isServiceEnabled) {
            statusText.text = getString(R.string.service_status_running)
        } else {
            statusText.text = getString(R.string.service_status_stopped)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (enabledService in enabledServices) {
            val serviceId = enabledService.id
            if (serviceId.contains(DimmingService::class.java.simpleName)) {
                return true
            }
        }
        return false
    }
}


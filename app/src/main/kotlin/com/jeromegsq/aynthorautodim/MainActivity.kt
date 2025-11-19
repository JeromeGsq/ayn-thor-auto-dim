package com.jeromegsq.aynthorautodim

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var permissionButton: Button
    private lateinit var serviceButton: Button
    private lateinit var delayInput: EditText
    private lateinit var saveButton: Button
    private lateinit var opacitySeekBar: SeekBar
    private lateinit var opacityLabel: TextView
    private lateinit var trueBlackSwitch: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        permissionButton = findViewById(R.id.permissionButton)
        serviceButton = findViewById(R.id.serviceButton)
        delayInput = findViewById(R.id.delayInput)
        saveButton = findViewById(R.id.saveButton)
        opacitySeekBar = findViewById(R.id.opacitySeekBar)
        opacityLabel = findViewById(R.id.opacityLabel)
        trueBlackSwitch = findViewById(R.id.trueBlackSwitch)

        loadPreferences()

        saveButton.setOnClickListener {
            saveSettings()
        }
        
        opacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                opacityLabel.text = getString(R.string.opacity_label, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

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

    private fun loadPreferences() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        
        // Load Duration
        val savedDuration = prefs.getLong("inactivity_delay_ms", 3000L)
        delayInput.setText((savedDuration / 1000).toString())
        
        // Load Opacity
        val savedOpacity = prefs.getInt("overlay_opacity", 100)
        opacitySeekBar.progress = savedOpacity
        opacityLabel.text = getString(R.string.opacity_label, savedOpacity)
        
        // Load True Black Mode
        trueBlackSwitch.isChecked = prefs.getBoolean("true_black_mode", false)
    }

    private fun saveSettings() {
        val input = delayInput.text.toString()
        val seconds = input.toLongOrNull()
        val opacity = opacitySeekBar.progress
        val trueBlack = trueBlackSwitch.isChecked
        
        if (seconds != null && seconds > 0) {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putLong("inactivity_delay_ms", seconds * 1000)
                .putInt("overlay_opacity", opacity)
                .putBoolean("true_black_mode", trueBlack)
                .apply()
            Toast.makeText(this, R.string.saved_toast, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Invalid duration", Toast.LENGTH_SHORT).show()
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

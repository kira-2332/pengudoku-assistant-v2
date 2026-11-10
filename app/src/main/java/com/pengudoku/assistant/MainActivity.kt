
package com.pengudoku.assistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private val captureRequest = 1001
    private val overlayRequest = 1002

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(32, 32, 32, 32)

        val title = TextView(this)
        title.text = "Pengudoku Assistant"
        title.textSize = 25f
        title.gravity = Gravity.CENTER

        status = TextView(this)
        status.text = "Ready to start"
        status.textSize = 16f
        status.gravity = Gravity.CENTER

        val captureButton = Button(this)
        captureButton.text = "Allow Screen Capture"
        captureButton.setOnClickListener {
            requestScreenCapture()
        }

        val overlayButton = Button(this)
        overlayButton.text = "Allow Floating Overlay"
        overlayButton.setOnClickListener {
            requestOverlayPermission()
        }

        val startButton = Button(this)
        startButton.text = "Start Floating Penguin"
        startButton.setOnClickListener {
            startOverlay()
        }

        val stopButton = Button(this)
        stopButton.text = "Stop Floating Penguin"
        stopButton.setOnClickListener {
            stopService(
                Intent(this, OverlayService::class.java)
            )
            status.text = "Floating Penguin stopped"
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(captureButton)
        layout.addView(overlayButton)
        layout.addView(startButton)
        layout.addView(stopButton)

        setContentView(layout)
    }

    private fun requestScreenCapture() {
        val manager = getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        startActivityForResult(
            manager.createScreenCaptureIntent(),
            captureRequest
        )
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, overlayRequest)
        } else {
            status.text = "Overlay permission granted"
        }
    }

    private fun startOverlay() {
        if (Settings.canDrawOverlays(this)) {
            startService(
                Intent(this, OverlayService::class.java)
            )
            status.text = "Floating Penguin started"
        } else {
            status.text = "Please allow overlay permission first"
            requestOverlayPermission()
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == captureRequest) {
            status.text = if (resultCode == RESULT_OK) {
                "Screen capture permission granted"
            } else {
                "Screen capture permission denied"
            }
        }

        if (requestCode == overlayRequest) {
            status.text = if (Settings.canDrawOverlays(this)) {
                "Overlay permission granted. Press Start Floating Penguin."
            } else {
                "Overlay permission not granted"
            }
        }
    }
}

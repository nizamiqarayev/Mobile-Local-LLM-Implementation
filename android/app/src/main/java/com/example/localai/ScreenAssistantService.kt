package com.example.localai

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageButton
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

class ScreenAssistantService : AccessibilityService() {
    private lateinit var windowManager: WindowManager
    private var assistantButton: ImageButton? = null
    private var buttonParams: WindowManager.LayoutParams? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        showAssistantButton()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        assistantButton?.let { runCatching { windowManager.removeView(it) } }
        assistantButton = null
        super.onDestroy()
    }

    private fun showAssistantButton() {
        if (assistantButton != null) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val size = (56 * resources.displayMetrics.density).toInt()
        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = (12 * resources.displayMetrics.density).toInt()
        }

        val button = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            contentDescription = "Ask LocalAI about this screen"
            background = getDrawable(android.R.drawable.btn_default)
            elevation = 10 * resources.displayMetrics.density
            setOnTouchListener(MovableButtonTouchListener(params))
        }
        assistantButton = button
        buttonParams = params
        windowManager.addView(button, params)
    }

    private inner class MovableButtonTouchListener(
        private val params: WindowManager.LayoutParams,
    ) : View.OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var touchX = 0f
        private var touchY = 0f

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX - (event.rawX - touchX).toInt()
                    params.y = startY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    val moved = abs(event.rawX - touchX) + abs(event.rawY - touchY)
                    if (moved < 20 * resources.displayMetrics.density) captureCurrentScreen()
                    return true
                }
            }
            return false
        }
    }

    private fun captureCurrentScreen() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Toast.makeText(this, "Screen capture requires Android 11 or newer.", Toast.LENGTH_LONG).show()
            return
        }

        assistantButton?.visibility = View.INVISIBLE
        assistantButton?.postDelayed({
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(result: ScreenshotResult) {
                        val hardwareBuffer = result.hardwareBuffer
                        val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, result.colorSpace)
                            ?.copy(Bitmap.Config.ARGB_8888, false)
                        hardwareBuffer.close()
                        assistantButton?.visibility = View.VISIBLE

                        if (bitmap == null) {
                            showCaptureError("Could not decode the screenshot.")
                            return
                        }
                        runCatching { saveScreenshot(bitmap) }
                            .onSuccess(::openPrompt)
                            .onFailure { error ->
                                showCaptureError(
                                    "Could not save screenshot: ${error.message ?: "unknown error"}",
                                )
                            }
                    }

                    override fun onFailure(errorCode: Int) {
                        assistantButton?.visibility = View.VISIBLE
                        showCaptureError(
                            "Screenshot blocked by Android (error $errorCode). " +
                                "Protected apps cannot be captured.",
                        )
                    }
                },
            )
        }, 180)
    }

    private fun saveScreenshot(bitmap: Bitmap): File {
        val directory = File(cacheDir, "screen-assistant").apply { mkdirs() }
        return File(directory, "current-screen.png").also { file ->
            FileOutputStream(file).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        }
    }

    private fun openPrompt(file: File) {
        val intent = Intent(this, ScreenPromptActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(ScreenPromptActivity.EXTRA_SCREENSHOT_URI, file.toURI().toString())
        }
        startActivity(intent)
    }

    private fun showCaptureError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

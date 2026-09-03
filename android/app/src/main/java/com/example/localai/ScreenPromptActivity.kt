package com.example.localai

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.localai.inference.createAndroidEngine
import com.example.localai.ui.App
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ScreenPromptActivity : ComponentActivity() {
    private val engine by lazy { createAndroidEngine(applicationContext) }
    private var screenshot by mutableStateOf<ImageBitmap?>(null)
    private var screenText by mutableStateOf("")
    private var readingScreen by mutableStateOf(false)
    private var screenError by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App(
                engine = engine,
                screenshot = screenshot,
                screenContextText = screenText,
                screenContextLoading = readingScreen,
                screenContextError = screenError,
                closeEngineOnDispose = false,
            )
        }
        consumeScreenshot(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeScreenshot(intent)
    }

    private fun consumeScreenshot(intent: Intent) {
        val uri = intent.screenshotUri() ?: run {
            screenError = "No screenshot was provided."
            return
        }

        runCatching {
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: error("The screenshot could not be opened.")
        }.onSuccess { bitmap ->
            screenshot = bitmap.asImageBitmap()
            screenText = ""
            screenError = null
            readingScreen = true

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { result -> screenText = result.text.trim() }
                .addOnFailureListener { error ->
                    screenError = "Could not read screen text: ${error.message ?: "unknown error"}"
                }
                .addOnCompleteListener {
                    readingScreen = false
                    recognizer.close()
                }
        }.onFailure { error ->
            screenError = error.message ?: "The screenshot could not be opened."
        }
    }

    @Suppress("DEPRECATION")
    private fun Intent.screenshotUri(): Uri? {
        getStringExtra(EXTRA_SCREENSHOT_URI)?.let(Uri::parse)?.let { return it }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    companion object {
        const val EXTRA_SCREENSHOT_URI = "com.example.localai.extra.SCREENSHOT_URI"
    }
}

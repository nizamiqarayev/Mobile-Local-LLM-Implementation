package com.example.localai

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.localai.inference.createAndroidEngine
import com.example.localai.ui.App

class MainActivity : ComponentActivity() {
    private val engine by lazy { createAndroidEngine(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App(
                engine = engine,
                onEnableScreenAssistant = {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                closeEngineOnDispose = false,
            )
        }
    }

}

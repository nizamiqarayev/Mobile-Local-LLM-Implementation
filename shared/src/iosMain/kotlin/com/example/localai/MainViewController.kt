package com.example.localai

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.example.localai.inference.LocalLLMEngine
import com.example.localai.inference.MockLocalLLMEngine
import com.example.localai.ui.App

fun MainViewController() = ComposeUIViewController {
    val engine = remember { MockLocalLLMEngine() }
    App(engine = engine)
}

fun MainViewController(engine: LocalLLMEngine) = ComposeUIViewController {
    App(engine = engine)
}

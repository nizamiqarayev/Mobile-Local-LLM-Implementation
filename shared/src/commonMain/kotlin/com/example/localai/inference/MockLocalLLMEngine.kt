package com.example.localai.inference

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MockLocalLLMEngine : LocalLLMEngine {
    override val name = "Mock offline engine"
    override var isReady = false
        private set

    override suspend fun prepare() {
        delay(250)
        isReady = true
    }

    override fun generate(prompt: String): Flow<String> = flow {
        val response = "This is a local streaming placeholder for: “${prompt.trim()}”. " +
            "Replace the platform engine factory with a real on-device runtime."

        response.split(" ").forEach { word ->
            emit("$word ")
            delay(45)
        }
    }

    override fun cancel() = Unit
    override fun close() = Unit
}


package com.example.localai.inference

import kotlinx.coroutines.flow.Flow

/**
 * Shared runtime boundary. Platform launchers inject an Android or iOS implementation.
 * Platform implementations wrap llama.cpp while the mock keeps UI work lightweight.
 */
interface LocalLLMEngine {
    val name: String
    val isReady: Boolean

    suspend fun prepare()
    fun generate(prompt: String): Flow<String>
    fun cancel()
    fun close()
}

package com.example.localai.inference

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ObjC/Swift-friendly bridge from the shared Compose UI to the native iOS
 * llama.cpp wrapper. Swift owns the C API objects; Kotlin only adapts callbacks
 * into the same Flow-based interface used on Android.
 */
class IosLlamaCallbackEngine(
    override val name: String,
    private val prepareHandler: (((String?) -> Unit) -> Unit),
    private val generateHandler: ((String, (String) -> Unit, (String?) -> Unit) -> Unit),
    private val cancelHandler: (() -> Unit),
    private val closeHandler: (() -> Unit),
) : LocalLLMEngine {
    override var isReady = false
        private set

    override suspend fun prepare() {
        if (isReady) return

        suspendCancellableCoroutine { continuation ->
            prepareHandler { error ->
                if (!continuation.isActive) return@prepareHandler
                if (error == null) {
                    isReady = true
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(IllegalStateException(error))
                }
            }
        }
    }

    override fun generate(prompt: String): Flow<String> = callbackFlow {
        check(isReady) { "llama.cpp has not finished loading the model." }

        generateHandler(
            prompt,
            { token -> trySend(token) },
            { error ->
                if (error == null) {
                    close()
                } else {
                    close(IllegalStateException(error))
                }
            },
        )

        awaitClose { cancelHandler() }
    }

    override fun cancel() = cancelHandler()

    override fun close() {
        isReady = false
        closeHandler()
    }
}

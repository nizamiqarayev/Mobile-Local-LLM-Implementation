package com.example.localai.inference

import android.content.Context
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Android local inference backed by the official llama.cpp Android JNI binding.
 *
 * llama.cpp loads GGUF models from a filesystem path, so the bundled asset is
 * copied once into the app's private files directory before it is loaded.
 */
class AndroidLlamaCppEngine(
    context: Context,
    private val modelAssetPath: String = DEFAULT_MODEL_ASSET_PATH,
) : LocalLLMEngine {
    private val appContext = context.applicationContext
    private var inferenceEngine: InferenceEngine? = null

    override val name = "llama.cpp · local CPU"
    override var isReady = false
        private set

    override suspend fun prepare() = withContext(Dispatchers.IO) {
        if (isReady) return@withContext

        val modelFile = installBundledModel()
        val activeEngine = AiChat.getInferenceEngine(appContext)

        val initializedState = activeEngine.state
            .filter {
                it is InferenceEngine.State.Initialized ||
                    it is InferenceEngine.State.ModelReady ||
                    it is InferenceEngine.State.Error
            }
            .first()

        if (initializedState is InferenceEngine.State.Error) {
            throw initializedState.exception
        }

        if (initializedState !is InferenceEngine.State.ModelReady) {
            activeEngine.loadModel(modelFile.absolutePath)
        }

        inferenceEngine = activeEngine
        isReady = true
    }

    override fun generate(prompt: String): Flow<String> {
        check(isReady) { "llama.cpp has not finished loading the model." }
        val activeEngine = inferenceEngine
            ?: error("llama.cpp inference engine is unavailable.")

        return activeEngine
            .sendUserPrompt(prompt, predictLength = MAX_RESPONSE_TOKENS)
            .flowOn(Dispatchers.Default)
    }

    override fun cancel() {
        // Cancelling the coroutine collecting sendUserPrompt() stops generation.
    }

    override fun close() {
        isReady = false
        val activeEngine = inferenceEngine
        inferenceEngine = null
        if (activeEngine?.state?.value is InferenceEngine.State.ModelReady) {
            activeEngine.cleanUp()
        }
    }

    private fun installBundledModel(): File {
        val filename = modelAssetPath.substringAfterLast('/')
        val modelDirectory = File(appContext.filesDir, "models").apply { mkdirs() }
        val target = File(modelDirectory, filename)
        if (target.isFile && target.length() > 0L) return target

        val temporary = File(modelDirectory, "$filename.partial")
        appContext.assets.open(modelAssetPath).use { input ->
            temporary.outputStream().buffered().use { output ->
                input.copyTo(output, bufferSize = 1024 * 1024)
            }
        }

        check(temporary.renameTo(target)) {
            "Could not move the bundled GGUF model into private app storage."
        }
        return target
    }

    companion object {
        const val DEFAULT_MODEL_ASSET_PATH = "models/model.gguf"
        private const val MAX_RESPONSE_TOKENS = 512

        fun isModelBundled(context: Context): Boolean {
            val models = context.assets.list("models") ?: return false
            return "model.gguf" in models
        }
    }
}

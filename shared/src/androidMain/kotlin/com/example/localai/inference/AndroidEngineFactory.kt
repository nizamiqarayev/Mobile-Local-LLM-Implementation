package com.example.localai.inference

import android.content.Context

/** Uses the real runtime when a model is bundled; keeps development builds runnable otherwise. */
fun createAndroidEngine(context: Context): LocalLLMEngine {
    return if (AndroidLlamaCppEngine.isModelBundled(context)) {
        AndroidLlamaCppEngine(context)
    } else {
        MockLocalLLMEngine()
    }
}

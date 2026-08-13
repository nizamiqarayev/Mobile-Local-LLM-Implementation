package com.example.localai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.localai.inference.LocalLLMEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun App(engine: LocalLLMEngine) {
    var prompt by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Preparing local engine…") }
    var ready by remember { mutableStateOf(false) }
    var generating by remember { mutableStateOf(false) }
    var generationJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(engine) {
        runCatching { engine.prepare() }
            .onSuccess {
                ready = engine.isReady
                status = "Ready · ${engine.name}"
            }
            .onFailure { status = "Model preparation failed: ${it.message}" }
    }

    DisposableEffect(engine) {
        onDispose {
            generationJob?.cancel()
            engine.cancel()
            engine.close()
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .safeContentPadding()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("LocalAI", style = MaterialTheme.typography.headlineLarge)
                Text(status, color = MaterialTheme.colorScheme.primary)
                Text(
                    "The interface and state are shared with Compose Multiplatform. " +
                        "Inference stays native to each device.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    label = { Text("Ask the local model") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        enabled = ready && prompt.isNotBlank() && !generating,
                        onClick = {
                            output = ""
                            generating = true
                            status = "Generating locally…"
                            generationJob = scope.launch {
                                try {
                                    engine.generate(prompt).collect { output += it }
                                    status = "Ready · ${engine.name}"
                                } catch (_: CancellationException) {
                                    status = "Cancelled · ${engine.name}"
                                } catch (error: Throwable) {
                                    status = "Generation failed: ${error.message}"
                                } finally {
                                    generating = false
                                }
                            }
                        },
                    ) { Text("Generate") }

                    OutlinedButton(
                        enabled = generating,
                        onClick = {
                            generationJob?.cancel()
                            engine.cancel()
                            generating = false
                            status = "Cancelled · ${engine.name}"
                        },
                    ) { Text("Stop") }
                }

                if (output.isNotBlank()) {
                    Text("Response", style = MaterialTheme.typography.titleMedium)
                    Text(output)
                }
            }
        }
    }
}

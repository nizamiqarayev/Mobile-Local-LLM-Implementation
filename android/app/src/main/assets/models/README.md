# Bundled llama.cpp model

Place a quantized GGUF chat model here with this exact name:

```text
model.gguf
```

When that file exists, `createAndroidEngine()` automatically selects the real
llama.cpp engine. Without it, the application deliberately uses the streaming
mock so normal UI development remains fast.

llama.cpp requires a filesystem path, so the first launch copies this bundled
asset into private application storage. For a production model over 200 MB
distributed through Google Play, use an install-time Play Asset Delivery pack
instead of the base module.

For the Screen Assistant on devices limited to roughly 2–3 GB of available RAM,
the target vision model is `Qwen3-VL-2B-Instruct` with the language model at
`Q4_K_M` and its multimodal projector at `Q8_0`. Use these names:

```text
model.gguf
mmproj.gguf
```

Download `Qwen3VL-2B-Instruct-Q4_K_M.gguf` (1.11 GB) and
`mmproj-Qwen3VL-2B-Instruct-Q8_0.gguf` (445 MB) from
`Qwen/Qwen3-VL-2B-Instruct-GGUF`, then rename them as shown above.

Qwen3-VL was selected because its 2B variant is aimed at edge deployment and is
specifically strong at screenshots, UI elements, and multilingual OCR. Keep the
context at 4K or below and cap image tokens when the native vision path is
enabled to stay inside the memory target.

The current Android binding passes locally extracted screenshot text to
`model.gguf`; `mmproj.gguf` is the next native-runtime integration point. Until
that is wired, non-text visual details are not sent to the model.

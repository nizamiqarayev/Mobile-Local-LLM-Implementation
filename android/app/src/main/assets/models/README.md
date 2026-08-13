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

Recommended first model: `gemma-3-1b-it-Q4_K_M.gguf` from
`ggml-org/gemma-3-1b-it-GGUF` (about 806 MB). Rename it to `model.gguf`.

# Bundled llama.cpp model

Place the quantized GGUF file here with this exact name:

```text
model.gguf
```

The Xcode build copies it into `Models/model.gguf` inside the application
bundle. When the file is absent, the app deliberately uses the mock engine.

Recommended first model: `gemma-3-1b-it-Q4_K_M.gguf` from the official
`ggml-org/gemma-3-1b-it-GGUF` repository. Rename the downloaded file to
`model.gguf`.

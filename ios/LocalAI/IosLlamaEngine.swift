import Foundation

final class IosLlamaEngine: @unchecked Sendable {
    private let modelURL: URL
    private var context: LlamaContext?
    private var generationTask: Task<Void, Never>?

    init(modelURL: URL) {
        self.modelURL = modelURL
    }

    func prepare(completion: @escaping (String?) -> Void) {
        generationTask?.cancel()
        generationTask = Task {
            do {
                context = try LlamaContext.create_context(path: modelURL.path)
                await MainActor.run { completion(nil) }
            } catch {
                await MainActor.run {
                    completion("Could not load model.gguf: \(error.localizedDescription)")
                }
            }
        }
    }

    func generate(
        prompt: String,
        onToken: @escaping (String) -> Void,
        completion: @escaping (String?) -> Void
    ) {
        generationTask?.cancel()
        generationTask = Task { [weak self] in
            guard let self, let context else {
                await MainActor.run { completion("llama.cpp is not ready.") }
                return
            }

            await context.clear()

            // model.gguf is expected to be Gemma 3 Instruct. Its chat control
            // tokens are part of the tokenizer, so this remains fully local.
            let formattedPrompt =
                "<start_of_turn>user\n\(prompt)<end_of_turn>\n" +
                "<start_of_turn>model\n"
            await context.completion_init(text: formattedPrompt)

            while !Task.isCancelled, await !context.is_done {
                let token = await context.completion_loop()
                if !token.isEmpty {
                    await MainActor.run { onToken(token) }
                }
            }

            if Task.isCancelled {
                await MainActor.run { completion("Generation cancelled.") }
            } else {
                await MainActor.run { completion(nil) }
            }
        }
    }

    func cancel() {
        generationTask?.cancel()
        generationTask = nil
    }

    func close() {
        cancel()
        let activeContext = context
        context = nil
        Task { await activeContext?.clear() }
    }
}

enum BundledModelLocator {
    static var modelURL: URL? {
        Bundle.main.url(
            forResource: "model",
            withExtension: "gguf",
            subdirectory: "Models"
        )
    }
}

import Shared
import SwiftUI
import UIKit

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard)
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        guard let modelURL = BundledModelLocator.modelURL else {
            return MainViewControllerKt.MainViewController()
        }

        let nativeEngine = IosLlamaEngine(modelURL: modelURL)
        let engine = IosLlamaCallbackEngine(
            name: "llama.cpp · Metal",
            prepareHandler: { completion in
                nativeEngine.prepare { error in
                    _ = completion(error)
                }
            },
            generateHandler: { prompt, onToken, completion in
                nativeEngine.generate(
                    prompt: prompt,
                    onToken: { token in
                        _ = onToken(token)
                    },
                    completion: { error in
                        _ = completion(error)
                    }
                )
            },
            cancelHandler: {
                nativeEngine.cancel()
            },
            closeHandler: {
                nativeEngine.close()
            }
        )
        return MainViewControllerKt.MainViewController(engine: engine)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

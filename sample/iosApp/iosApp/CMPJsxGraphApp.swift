import CmpJsxGraphDebugUi
import SwiftUI
import UIKit

@main
struct CMPJsxGraphApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.keyboard)
        }
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        if ProcessInfo.processInfo.arguments.contains("--load-test") {
            return IosDebugUiKt.JsxGraphLoadViewController()
        }
        return IosDebugUiKt.JsxGraphDebugViewController()
    }

    func updateUIViewController(
        _ uiViewController: UIViewController,
        context: Context
    ) {
    }
}

import SwiftUI
import UIKit
import MoversShared

@main
struct MoversApp: App {
    init() {
        let token = Bundle.main.object(forInfoDictionaryKey: "TMDB_API_TOKEN") as? String ?? ""
        MainViewControllerKt.initializeIosMovers(apiToken: token)
    }

    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea()
        }
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ controller: UIViewController, context: Context) { }
}

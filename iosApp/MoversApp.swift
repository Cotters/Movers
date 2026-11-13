import MoversShared
import SwiftUI
import UIKit

@main
struct MoversApp: App {
    @UIApplicationDelegateAdaptor(MoversNotificationDelegate.self) private var notifications

    init() {
        let token = Bundle.main.object(forInfoDictionaryKey: "TMDB_API_TOKEN") as? String ?? ""
        let backendUrl = Bundle.main.object(forInfoDictionaryKey: "SUPABASE_URL") as? String ?? ""
        let backendKey = Bundle.main.object(forInfoDictionaryKey: "SUPABASE_PUBLISHABLE_KEY") as? String ?? ""
        MainViewControllerKt.initializeIosMovers(apiToken: token, backendUrl: backendUrl, backendKey: backendKey)
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea()
                .onOpenURL { url in
                    IncomingInvitationKt.receiveInvitation(url: url.absoluteString)
                }
        }
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ controller: UIViewController, context: Context) {}
}

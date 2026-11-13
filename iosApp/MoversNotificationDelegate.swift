import MoversShared
import UIKit
import UserNotifications

final class MoversNotificationDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    private var authorization: UNAuthorizationStatus = .notDetermined
    private var token: String?
    private var registrationError: String?
    private var registrationInFlight = false
    private var registeredThisLaunch = false
    private var requesting = false
    private var permissionTask: Task<Void, Never>?
    private var refreshTask: Task<Void, Never>?
    private let environment = PushEnvironment.current()
    private let deviceId: String = {
        let defaults = UserDefaults.standard
        if let existing = defaults.string(forKey: "movers.push.device-id") { return existing }
        let value = UUID().uuidString.lowercased()
        defaults.set(value, forKey: "movers.push.device-id")
        return value
    }()

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        IosNotificationsKt.configureIosNotifications(
            requestPermission: { [weak self] in self?.requestWhenReady() },
            openSettings: {
                guard let url = URL(string: UIApplication.openNotificationSettingsURLString) else { return }
                UIApplication.shared.open(url)
            },
            refresh: { [weak self] in self?.refresh() }
        )
        refresh()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        token = deviceToken.map { String(format: "%02x", $0) }.joined()
        registrationInFlight = false
        registeredThisLaunch = true
        registrationError = nil
        report()
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        registrationInFlight = false
        registrationError = "Couldn't register with Apple. Check your connection and retry."
        report()
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let payload = response.notification.request.content.userInfo
        guard response.actionIdentifier == UNNotificationDefaultActionIdentifier,
            let version = payload["version"] as? Int, version == 1,
            let type = payload["type"] as? String,
            let listId = payload["listId"] as? String,
            let groupId = payload["groupId"] as? String
        else {
            completionHandler()
            return
        }
        DispatchQueue.main.async {
            _ = IosNotificationsKt.receiveWatchlistNotification(
                version: Int32(version), type: type, listId: listId, groupId: groupId
            )
            completionHandler()
        }
    }

    private func refresh() {
        refreshTask?.cancel()
        refreshTask = Task { @MainActor [weak self] in
            let settings = await UNUserNotificationCenter.current().notificationSettings()
            guard !Task.isCancelled, let self else { return }
            self.authorization = settings.authorizationStatus
            if !self.isAllowed { self.registrationError = nil }
            if self.isAllowed && (!self.registeredThisLaunch || self.registrationError != nil),
                !self.registrationInFlight, self.environment != nil
            {
                self.registrationInFlight = true
                self.registrationError = nil
                UIApplication.shared.registerForRemoteNotifications()
            }
            self.report()
        }
    }

    private var isAllowed: Bool {
        authorization == .authorized || authorization == .provisional || authorization == .ephemeral
    }

    private func report() {
        let state = isAllowed ? "allowed" : (authorization == .denied ? "denied" : "notDetermined")
        let error =
            environment == nil ? "Notifications are unavailable in this app build." : registrationError
        IosNotificationsKt.receiveIosNotificationStatus(
            authorization: state, deviceId: deviceId, environment: environment ?? "", token: token, error: error
        )
    }

    private func requestWhenReady() {
        guard !requesting else { return }
        requesting = true
        permissionTask = Task { @MainActor [weak self] in
            while !Task.isCancelled {
                guard let self else { return }
                if UIApplication.shared.applicationState == .active && !self.hasPresentedController {
                    do {
                        _ = try await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound])
                    } catch {
                        self.registrationError = "Couldn't request notifications. Try again."
                    }
                    self.requesting = false
                    self.refresh()
                    return
                }
                do { try await Task.sleep(nanoseconds: 500_000_000) } catch { return }
            }
        }
    }

    private var hasPresentedController: Bool {
        UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows).contains { $0.isKeyWindow && $0.rootViewController?.presentedViewController != nil }
    }
}

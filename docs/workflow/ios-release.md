# iOS build and release

Use the tracked `iosApp/Movers.xcodeproj` and **Movers** scheme. The app minimum is **iOS 16.0** in both `iosApp/project.yml` and the Xcode project. Keep them aligned; review generated changes when using XcodeGen. The Swift entry point, `iosApp/MoversApp.swift`, initializes shared services, embeds Compose and forwards invitation links. Keep it tracked.

## Compile verification

Use macOS, full Xcode with the iOS device SDK, and JDK 17. Configure your own ignored `iosApp/Local.xcconfig` using its example for live data and signing. Placeholder configuration permits compilation only. See [platform setup](multiplatform.md#configure-and-build).

When requested or needed, run these sequentially from the repository root:

```sh
./scripts/verify-ios.sh
./scripts/verify-ios.sh --archive
```

The script checks prerequisites and strict Swift formatting. Without `JAVA_HOME`, it selects an installed JDK 17; an explicit setting must also be 17. Debug builds the complete Swift/Compose app under `build/iosDerivedData`. Release additionally exercises Kotlin/Native optimization and creates **unsigned** `build/iosArchives/Movers.xcarchive`. Neither command installs, launches, signs for distribution, exports or uploads. A framework link alone cannot validate the Swift host.

Review the actual app's `Info.plist` version/build, bundle identifier and `MinimumOSVersion`, the executable's deployment target (`xcrun vtool -show-build`), Compose resources, app icon and privacy manifest. Inspect only allowlisted metadata: the plist and raw build logs also contain local configuration. Record the checkpoint, command, result and warnings in the [handoff](handoff.md). Keep logs/archives under ignored `build/`; never commit credentials or signing artifacts.

For Swift-only source review without a build:

```sh
xcrun swift-format lint --strict iosApp/MoversApp.swift
```

The root `.swift-format` defines four-space indentation and 120-character lines. Include `iosApp/MoversNotificationDelegate.swift` and `iosApp/PushEnvironment.swift` when reviewing notification changes; `verify-ios.sh` lints all three host files. Use `xcrun swift-format format --in-place` on explicit task-owned files, then inspect the diff. Follow the normal [code review](code-review.md) too.

## Release delivery

1. Agree release scope. Run the existing host suite and full iOS Debug/Release verification for the release checkpoint. Review warnings; do not suppress them or infer runtime compatibility from a successful link.
2. Have the owner test on a physical iPhone: launch/relaunch, details/Back, retained tabs, search/keyboard, list add/remove/Undo and sharing/invitation links. For the iOS 16 support claim, include a device running iOS 16; a newer phone cannot establish that minimum. Record device/OS and results. Agents never use simulators or execute connected tests.
3. Use semantic **major.minor.patch** marketing versions: major for breaking changes, minor for compatible features, patch for fixes. New features must not be labelled as patch releases. The numeric upload/build counter increases independently; verify the next unused value in ASC. Update Android and iOS XcodeGen/project settings together. The prepared notification feature release is **0.3.0 (4)**; the latest uploaded build remains **0.2.0 (3)**. A version bump does not authorize publication.
4. With the local signing team configured, select a generic iOS device in Xcode and use **Product → Archive**, then Organizer → **Distribute App → App Store Connect → Upload**. Reuse the existing `com.jcotters.movers` app record. Build a fresh signed archive; the verification archive is unsigned. Check the signed artifact's identifier, version/build and distribution profile. The existing [ASC CLI route](multiplatform.md#app-store-connect-and-uploads) is an alternative; discover flags from the installed CLI.
5. Verify App Store Connect processing is **Valid**, resolve export-compliance information, and check the intended TestFlight groups. External beta review, tester availability and public App Store release are separate states/actions. Report the actual state; upload success alone does not mean testers have it.

No automatic signing, upload or release is configured in CI. Local build verification does not establish hosted CI, signing validity or Apple acceptance. Obtain release authorization for delivery; preserve the existing tester audiences and private credentials.

References checked 13 November 2026: [Compose/SwiftUI integration](https://kotlinlang.org/docs/multiplatform/compose-swiftui-integration.html), [Kotlin direct Xcode integration](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html), [Apple archive workflow](https://help.apple.com/xcode/mac/current/en.lproj/devf37a1db04.html), [Swift formatter](https://github.com/swiftlang/swift-format).

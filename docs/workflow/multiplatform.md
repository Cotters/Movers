# Android and iOS

Movers shares its screens, navigation, ViewModels, repositories, HTTP policy, cache, and Room schema. Android and iOS hosts supply platform services. Kotlin is 2.4.21; Compose Multiplatform is 1.12.2. Keep the current catalog authoritative and fix demonstrated compatibility failures rather than blocking work on theoretical matrix mismatches.

## Ownership

| Location | Responsibility |
| --- | --- |
| `:app` | Android application/activity, system bars, and platform startup |
| `iosApp/` | SwiftUI app embedding the shared Compose controller; Xcode project and local configuration |
| `:shared` | App composition, Koin graph, theme, retained bottom-navigation state, and platform service bindings |
| `:designsystem`, `:features:styleguide` + contract | Shared production tokens/components and development-only gallery; 16 dp corners/Comfortable defaults and local preview overrides |
| Existing feature modules and contracts | Shared screens/state/use cases and feature-owned NavKeys |
| `:profile:contract`, `:profile` | Device-profile model/repository contract, local implementation, and Profile UI |
| `:movie:contract`, `:movie:internal` | Public movie/search/list operations and neutral list storage API; Ktor transport, mapping, paging, freshness, and repository implementations |
| `:database` | Room entities/DAOs, version 9 schema with preserving 7→8→9 migrations, bundled SQLite, and platform database paths |
| `:backend:supabase` | Supabase SDK/remote operations, anonymous identity and native secure session storage behind neutral contracts |
| `:presentation`, `:data:network` | Shared images/transitions/snackbars and network-status contract; native network observers |

Use `src/commonMain/kotlin` and `composeResources` for reusable code/assets. `androidMain` and `iosMain` contain native integrations. Only the composition module creates the iOS framework; other modules remain libraries. iOS targets are `iosArm64` for physical devices and `iosSimulatorArm64` for Apple silicon simulators, minimum iOS 15. No Intel simulator or desktop target is configured. Room's KSP processor is configured for both iOS targets.

Koin definitions stay at composition boundaries; constructors receive typed dependencies. Feature entry providers own their screens. ViewModels use the Nav 3 entry's store, not a global singleton. Navigation uses an explicit polymorphic serializer registry on both platforms; register new keys when adding destinations. Do not reintroduce Android reflection-based serialization in common code.

Retain the existing Home feed, details/retry/cache behavior, custom Watchlists/search, and Profile. Each tab keeps its history. Reselection pops to that tab's root; Back from another tab root returns Home. Android Back at Home root exits. Error messages use the shared snackbar host with Movers notices. The brand palette and Inter typography are shared; system light/dark mode is respected without Android dynamic-colour overrides.

## Device profile and reset

Account login, passwords, saved-credential prompts and logout are removed. Room stores one random installation ID, a confirmed name and a randomly assigned avatar color. First launch asks for a name; existing Josh profiles confirm or replace it once. Initialization is idempotent. Shared Lists additionally use a securely persisted anonymous backend identity behind `ParticipantIdentity`; names and installation IDs are not authorization credentials.

The user explicitly authorized discarding old local data for this migration. Version 7 resets databases from versions 1–6, including old accounts/bookmarks/cache. Android also removes retired credential/session preference files and rejects old navigation saved state. Versions 8–10 preserve earlier profile/list data through explicit migrations; 9→10 drops only the retired bookmark table; do not broaden the destructive fallback. Old schema exports remain historical records.

First-launch name entry is followed by a short optional [feature tour](../features/onboarding.md). Completion/Skip persists in installation-local native preferences; it never replaces the confirmed profile or navigation state. See the [agreed design](../features/shared-watchlists.md), [backend setup](../../supabase/README.md), and [vendor-neutral boundary](open-source.md). Profile offers the existing eight avatar colors; the selected index persists and uses the neutral identity update for existing shared sessions. Identity recovery/device linking remains deferred.

## Configure and build

Build only when requested or concretely necessary for a substantial change/release; ordinary iteration uses static checks. The commands below are available tools, not automatic gates. Use JDK 17 and the SDK prerequisites in [testing](testing.md). Android uses an ignored `movie/internal/src/androidMain/kotlin/com/jcotters/internal/ApiConstants.kt`; preserve existing credentials and use the placeholder example in the testing guide for host checks. No token is compiled into common Kotlin sources.

For iOS, copy `iosApp/Local.xcconfig.example` to ignored `iosApp/Local.xcconfig` and supply your own TMDB read-access token. Without it, the placeholder permits compilation but cannot load live movie data. Set `DEVELOPMENT_TEAM` in that same ignored file for physical-device/TestFlight signing; keep personal team IDs out of the project and `project.yml`. Do not commit or share the local file or unreviewed build logs. Live backend settings, Apple team IDs, signing credentials and privileged keys stay local.

Open `iosApp/Movers.xcodeproj`, choose the Movers scheme and a physical iPhone. Automatic signing uses your local team setting. If Xcode writes a team ID into `project.pbxproj`, move it back to the ignored config before committing. The build phase invokes Gradle to build/copy the shared framework and Compose resources. The tracked project is ready to open; XcodeGen is only needed after editing `project.yml`:

```sh
xcodegen generate --spec iosApp/project.yml --project iosApp
./scripts/verify.sh
./scripts/verify-ios.sh
```

The iOS verification script builds unsigned with the physical-device SDK; it does not install or launch anything. `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` compiles the arm64 Simulator framework without launching anything. Agents never run or interact with emulators/simulators under the latest standing instruction. Native runtime/UI verification remains user-owned on physical devices. Android PR CI runs the host script with a placeholder token. iOS CI, signing, distribution, and runtime acceptance are not implied by a local build.

For archive readiness, run `./scripts/verify-ios.sh --archive` as a separate check. It exercises Release optimization and writes an unsigned archive to `build/iosArchives/Movers.xcarchive`; use Xcode's signed Archive flow for actual device/TestFlight distribution. See [verification](testing.md#shared-androidios-verification) for heap settings and the difference between Debug and archive coverage.

### App Store Connect and uploads

The app record now exists as **Movers: Shared Watchlists**, with bundle ID and SKU `com.jcotters.movers`. The latest Xcode log records successful creation (HTTP 201), and ASC CLI independently confirms it. A repeated SKU warning during another creation attempt is not a reason to change the bundle ID: reuse the existing record. See Apple's [app-record setup](https://developer.apple.com/help/app-store-connect/create-an-app-record/add-a-new-app/) and [bundle-ID/SKU requirements](https://developer.apple.com/help/app-store-connect/reference/app-information/app-information/).

Android and iOS marketing versions are **0.2.0**, with numeric build counter **3**. Keep the counter monotonic for later uploads. `APP_DISPLAY_NAME` in `iosApp/Configuration.xcconfig` supplies the iOS display name; plist and XcodeGen reference it. Plist version fields reference `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION`, preventing stale literal values in archives. The internal product name remains Movers.

Both normal Xcode Organizer upload and ASC CLI upload are supported. The verified CLI pipeline is:

1. Run existing host verification, then `asc xcode archive` for the Movers scheme in Release configuration, with a generic iOS destination and a distinct ignored archive path. The verification script's unsigned archive is a separate build check.
2. Generate export options with `asc xcode export-options generate`, writing them under ignored `build/`. Use `method=app-store-connect`, `destination=export`, automatic signing and `manageAppVersionAndBuildNumber=false` to preserve the verified version/build.
3. Run `asc xcode export` with those options and `--xcodebuild-flag=-allowProvisioningUpdates`. Xcode's signing account/provisioning setup must work; check the resulting IPA's signature, App Store profile and metadata before upload.
4. Upload the signed IPA through the working API-key authentication and wait for processing:

```sh
asc builds upload --app com.jcotters.movers   --ipa PATH_TO_SIGNED_IPA --checksum --wait
```

Discover flags through the installed command's `--help`; this flow was verified with ASC CLI 5.1. The CLI's separate Apple web-login path returned HTTP 404 fetching its auth-service key, but that did not prevent signed export or API upload. Do not request repeated web logins for an existing app's upload. Confirm `VALID` processing with `asc builds list` before declaring success; an upload is separate from review submission, tester distribution and store publication.

For TestFlight, inspect build beta details after processing. Version 0.2.0 (3) needed its existing exempt-encryption declaration applied with `asc builds update --build-id BUILD_ID --uses-non-exempt-encryption=false`; reassess if the app's cryptography changes. Then use `asc publish testflight --app APP_ID --build-id BUILD_ID --group EXTERNAL_GROUP_ID --submit --confirm --wait` for an authorized external beta release. Verify group linkage and beta-review state separately: `WAITING_FOR_REVIEW` is pending Apple approval, not external availability. Internal groups with all-build access automatically receive eligible builds; Apple rejects explicit build assignment to these groups. Preserve existing audiences/public-link settings. Normal App Store review/publication remains a separate action. See [Apple's beta distribution guidance](https://developer.apple.com/help/app-store-connect/test-a-beta-version/add-testers-to-builds) and [encryption declaration guidance](https://developer.apple.com/documentation/Security/complying-with-encryption-export-regulations).

For Organizer, create a fresh signed archive with Product → Archive, then choose Distribute App → App Store Connect → Upload. Use the existing record. If the failed creation flow remains open, close it and begin distribution again, checking the selected account/team.

Keep distribution logs, `.asc/config.json`, private keys, provisioning profiles, export options, archives and IPAs out of Git. Inspect logs with sanitized output. No real signing identifiers or credentials belong in public setup examples.

## Verification and physical-device acceptance

Host checks cover shared policy, transport fixtures, repositories, profile initialization, navigation serialization, and real Compose navigation journeys at phone/Fold sizes. Run existing tests and maintain intended expectations; new cases are deferred during the feature phase. See the [host UI gate](testing.md#host-ui-regression-gate). Android device tests are compiled with:

```sh
./gradlew :shared:assembleAndroidDeviceTest :movie:internal:assembleAndroidDeviceTest
```

They cover retained navigation/ViewModel stores/restoration, screen loading/error states, movie enrichment/list relationships/cache transactions, and the one-time reset followed by persistence. They are not executed by agents.

On your own Android device and iPhone, check Home loading and infinite scroll, a movie's details and retry, Add to list and saved memberships, Profile name editing and confirmed name/avatar, all three tabs and retained histories, and relaunch persistence. Check offline startup, cached content, reconnect/retry, system insets, keyboard-free navigation, and light/dark themes. On Android, check Back/reselection/process restoration and run the compiled integration tests. Confirm the authorized reset on an old Android install using disposable data. Send device/OS versions and sanitized observations/screenshots/logs; keep runtime acceptance pending until evidence arrives.

Official references: [incremental migration](https://kotlinlang.org/docs/multiplatform/migrate-from-android.html), [Android KMP plugin](https://developer.android.com/kotlin/multiplatform/plugin), [Nav 3 serialization](https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html), [Koin Compose](https://insert-koin.io/docs/reference/koin-compose/compose/), and [Room KMP](https://developer.android.com/kotlin/multiplatform/room). The current Room guide contains Room 3 prerelease snippets; Movers intentionally retains Room 2.8.5. Match installed APIs rather than copying package/version changes incidentally.

## Changes to shared code

KMP supplies shared code/logic and target integrations; Compose Multiplatform supplies shared UI. For a feature, keep reusable policy/state/UI in the existing common source sets and inject small native integrations. Do not add platform targets or new abstraction layers solely to satisfy an agent tool's default example.

- Follow the dependency actually consumed by the module. Shared navigation uses the JetBrains artifact through `libs.multiplatform.navigation3.ui`; Android and shared catalog entries can have different versions. Likewise Material 3, Compose UI and lifecycle versions are independent.
- Review common sources and public contracts for Android/JVM/provider-only types. Preserve entry-scoped ViewModels, explicit NavKey serializers and Koin bindings on both hosts. Verify packaged resources when assets/conventions change; compilation/accessors alone do not establish packaging or portable XML parsing.
- Apply the [requested/necessary build policy](testing.md#verification-by-change); no automatic gates during iteration. When verification is requested/necessary, select affected host/native checks. For target, KSP, resource-packaging or convention changes, consider affected configured targets, including `:shared:linkDebugFrameworkIosSimulatorArm64` when affected. This builds a framework without starting a simulator. Compile relevant Android test APKs separately.
- Record each target and result separately. Robolectric is Android host simulation; it does not execute iOS. Linking a framework or preparing simulator resources does not prove native launch/runtime behavior.
- Keep platform warnings visible. The historical bundled ICU Simulator object warning (built for 18.5; configured minimum 15.0) remains a release/compatibility follow-up; successful newer-OS validation does not establish minimum-OS support.

See [Kotlin's source-set guidance](https://kotlinlang.org/docs/multiplatform/multiplatform-share-on-platforms.html) and [AI-specific Kotlin acceptance guidance](https://kotlinlang.org/docs/ai-for-development.html#kotlin-specific-acceptance-criteria). Apply target checks within the user's no-device and existing-tests-only policies.

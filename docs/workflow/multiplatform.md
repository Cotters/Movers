# Android and iOS

Movers shares its screens, navigation, ViewModels, repositories, HTTP policy, cache, and Room schema. Android and iOS hosts supply platform services. Kotlin is 2.4.21; Compose Multiplatform is 1.12.2. Keep the current catalog authoritative and fix demonstrated compatibility failures rather than blocking work on theoretical matrix mismatches.

## Ownership

| Location | Responsibility |
| --- | --- |
| `:app` | Android application/activity, system bars, and platform startup |
| `iosApp/` | SwiftUI app embedding the shared Compose controller; Xcode project and local configuration |
| `:shared` | App composition, Koin graph, theme, retained bottom-navigation state, and platform service bindings |
| Existing feature modules and contracts | Shared screens/state/use cases and feature-owned NavKeys |
| `:profile:contract`, `:profile` | Device-profile model/repository contract, local implementation, and Profile UI |
| `:movie:contract`, `:movie:internal` | Public movie/bookmark operations; Ktor transport, mapping, paging, freshness, and repository implementations |
| `:database` | Room entities/DAOs, version 7 schema, bundled SQLite, and platform database paths |
| `:presentation`, `:data:network` | Shared images/transitions/snackbars and network-status contract; native network observers |

Use `src/commonMain/kotlin` and `composeResources` for reusable code/assets. `androidMain` and `iosMain` contain native integrations. Only the composition module creates the iOS framework; other modules remain libraries. The current iOS target is `iosArm64` for physical devices, minimum iOS 15. No simulator or desktop target is configured.

Koin definitions stay at composition boundaries; constructors receive typed dependencies. Feature entry providers own their screens. ViewModels use the Nav 3 entry's store, not a global singleton. Navigation uses an explicit polymorphic serializer registry on both platforms; register new keys when adding destinations. Do not reintroduce Android reflection-based serialization in common code.

Retain the existing Home feed, details/retry/cache behavior, bookmarks, Watchlists placeholder, and Profile. Each tab keeps its history. Reselection pops to that tab's root; Back from another tab root returns Home. Android Back at Home root exits. Error messages use the shared snackbar; Android dynamic colors remain platform-specific.

## Device profile and reset

Accounts, passwords, saved-credential prompts, logout, and session expiry are removed. Room stores one random installation ID and the temporary name **Josh**. Initialization is idempotent; reopening the app keeps that identity and new bookmarks. Names and installation IDs are not remote authorization credentials.

The user explicitly authorized discarding old local data for this migration. Version 7 resets databases from versions 1–6, including old accounts/bookmarks/cache. Android also removes retired credential/session preference files and rejects old navigation saved state. Future version-7 data requires normal preservation/migration; do not broaden the destructive fallback. Old schema exports remain historical records.

**Onboarding is out of scope.** The private Todo records first-launch name entry and a feature introduction to replace the temporary Josh seed. No name-entry or account UI should be added incidentally. Shared Lists and backend identity/authorization remain separate planned work; retain the [vendor-neutral database boundary](open-source.md).

## Configure and build

Use JDK 17 and the SDK prerequisites in [testing](testing.md). Android uses an ignored `movie/internal/src/androidMain/kotlin/com/jcotters/internal/ApiConstants.kt`; preserve existing credentials and use the placeholder example in the testing guide for host checks. No token is compiled into common Kotlin sources.

For iOS, copy `iosApp/Local.xcconfig.example` to ignored `iosApp/Local.xcconfig` and supply your own TMDB read-access token. Without it, the placeholder permits compilation but cannot load live movie data. Do not commit or share the local file or unreviewed build logs. No backend, Apple team, signing credential, or privileged key is tracked.

Open `iosApp/Movers.xcodeproj`, choose the Movers scheme and a physical iPhone, then configure your signing team locally. The build phase invokes Gradle to build/copy the shared framework and Compose resources. The tracked project is ready to open; XcodeGen is only needed after editing `project.yml`:

```sh
xcodegen generate --spec iosApp/project.yml --project iosApp
./scripts/verify.sh
./scripts/verify-ios.sh
```

The iOS verification script builds unsigned with the physical-device SDK; it does not install or launch anything. Android PR CI runs the host script with a placeholder token. iOS CI, signing, distribution, and runtime acceptance are not implied by a local build.

## Verification and physical-device acceptance

Host checks cover shared policy, transport fixtures, repositories, profile initialization, navigation serialization, and real Compose navigation journeys at phone/Fold sizes. Relevant host UI tests must pass before shared UI/navigation work is complete; see the [host UI gate](testing.md#host-ui-regression-gate). Android device tests are compiled with:

```sh
./gradlew :shared:assembleAndroidDeviceTest :movie:internal:assembleAndroidDeviceTest
```

They cover retained navigation/ViewModel stores/restoration, screen loading/error states, observable/idempotent bookmarks, movie enrichment/cache transactions, and the one-time reset followed by persistence. They are not executed by agents.

On your own Android device and iPhone, check Home loading and infinite scroll, a movie's details and retry, bookmark/unbookmark and Profile updates, the Josh name, all three tabs and retained histories, and relaunch persistence. Check offline startup, cached content, reconnect/retry, system insets, keyboard-free navigation, and light/dark themes. On Android, check Back/reselection/process restoration and run the compiled integration tests. Confirm the authorized reset on an old Android install using disposable data. Send device/OS versions and sanitized observations/screenshots/logs; keep runtime acceptance pending until evidence arrives.

Official references: [incremental migration](https://kotlinlang.org/docs/multiplatform/migrate-from-android.html), [Android KMP plugin](https://developer.android.com/kotlin/multiplatform/plugin), [Nav 3 serialization](https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html), [Koin Compose](https://insert-koin.io/docs/reference/koin-compose/compose/), and [Room KMP](https://developer.android.com/kotlin/multiplatform/room). The current Room guide contains Room 3 prerelease snippets; Movers intentionally retains Room 2.8.5. Match installed APIs rather than copying package/version changes incidentally.

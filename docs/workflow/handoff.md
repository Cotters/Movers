# Current handoff

Updated: 9 November 2026. Read the current source and Git state before resuming; reports from earlier architecture versions do not verify later changes.

## Objective and status

The Android/iOS KMP and Compose Multiplatform migration is implemented from baseline `50a788b` (`Update deps`). Existing feature/contract and movie contract/internal boundaries remain. Shared screens, Nav 3 navigation, ViewModels, repositories, Ktor transport, paging/cache, and Room storage serve a thin Android app and a SwiftUI iOS host. Koin replaces Hilt; accounts are removed. See [ownership, setup, and device acceptance](multiplatform.md) and [build conventions](../../build-logic/README.md).

Android host verification and the unsigned physical-iOS app build pass. Runtime acceptance on physical Android/iPhone devices, IDE sync, signing, and remote GitHub CI remain pending. No emulator/simulator or connected test was used during the initial migration. Subsequent explicitly authorized debugging exceptions and the switch to host UI tests are recorded below. The user committed the migration and host UI setup as `c21bd56`; earlier references to uncommitted migration code are historical. Recheck Git before resuming.

## Local watchlists implementation — 9 November 2026

The agreed local-list plan is implemented against `d3a7d97` plus the working tree. Preserve the user's staging; no commit or index update was performed by the agent.

- Watchlists supports create/rename/delete and movie removal. Names trim/nonblank; equal names remain allowed with a case-insensitive, non-blocking warning. Lists and memberships are newest first.
- Home has global search. A list's + opens targeted search; Add saves directly to that list, becomes disabled Added, and keeps the search/query open for consecutive additions. Search debounces typing by 600 ms, supports immediate keyboard submission, skips blank/unchanged queries, and replaces stale searches. Paging failures offer explicit retry; no automatic service retry loop.
- Catalogue, details, and global search have quick-add. The Nav 3 bottom sheet permits selecting several destination lists for one film, disables existing memberships, and allows inline list creation/automatic selection. An atomic successful add returns to the retained source. Failure retains selection; a created list survives cancelling the picker. Entry-scoped ViewModels and explicit shared key serialization remain in use.
- Public `IWatchlistsRepository` depends on a separate neutral `WatchlistDatabase`, with an internal Room adapter selected by Koin. Search summaries persist before adding and preserve enrichment/freshness; catalogue membership and bookmarks remain independent.
- Room schema **8** includes an explicit preserving **7→8** migration. The destructive fallback still applies only to retired versions 1–6. Host tests migrate a populated exported schema, reopen saved data, exercise real relationships/idempotence, and force the second insert of a multi-list operation to fail to prove rollback.
- List-name dialogs have explicit shared width bounds. The host regression exposed a native dialog measurement stall with implicit platform width; the complete CRUD journey passes with ordinary V2 Compose test synchronization. Temporary diagnostics and clock/timeout experiments were removed.
- Collaboration, sharing links, Supabase, backend identity, onboarding, and bookmark redesign remain out of scope. Local lists require no backend configuration. Fresh movie search needs TMDB/network; persisted lists are available offline.

Verification: `./scripts/verify.sh` passed with **92 host tests across 24 suites**, zero failures/errors/skips; five convention tests reused passing outputs. Eight real-app host UI cases include the five new list/search journeys and three catalogue regressions. Lint/debug APK pass with 30 warnings and no errors. The unsigned physical-iOS build passes. Release and both retained Android integration-test APKs compile; they were not executed. Shell, resource/schema, local documentation links, and Maestro syntax checks are recorded in the final handoff evidence below.

Final verification evidence for this working tree (9 November):

- `./scripts/verify.sh`: convention phase reused 13 up-to-date tasks; app phase passed in 40 seconds, 44 tasks executed and 495 up-to-date. Shared UI/storage and movie-internal tests executed in that final run; other passing unit suites reused outputs. Reports total 92 tests/24 suites, zero failures/errors/skips, plus five convention tests. Lint has 30 warnings and zero errors.
- `./scripts/verify-ios.sh`: exit 0, unsigned `iphoneos` app. The final Gradle phase took 30 seconds (36 executed/140 up-to-date), compiling changed shared/native modules and linking `linkDebugFrameworkIosArm64`. Simulator-resource preparation is a build task, not simulator execution.
- `./gradlew :app:assembleRelease :shared:assembleAndroidDeviceTest :movie:internal:assembleAndroidDeviceTest --console=plain`: passed after updating the catalogue test's API fake for the added search operation; 32 tasks executed/540 up-to-date. Compilation/packaging only, no instrumented execution.
- Both Maestro YAML files pass CLI 2.12.1 `check-syntax`; the retained suite was not run natively. Verification scripts pass `bash -n`, 41 local documentation links resolve, shared vectors/schema JSON parse, and all four new vectors are present in the debug APK. `git diff --check` passes. Human.md is 200 words with feedback preserved; credentials/private Notes and generated test artifacts remain ignored.

Native/UI acceptance remains **pending on physical devices**, per the latest standing instruction. No emulator, simulator, connected test, device install, or manual UI interaction was used for this change. The retained native suite now has separate catalogue and watchlist flows and runs the directory; syntax validation alone is not a native pass. The watchlist flow creates timestamped test lists and removes only those lists; interrupted runs can leave them behind.

Next: the user checks [the list/search device checklist](navigation.md#physical-device-acceptance) on Android and iPhone, including keyboard/small-screen layout, source retention, offline/retry, and relaunch persistence. Report device/OS versions and sanitized observations/logs. Plan sharing/authorization separately after local acceptance.

## Accepted decisions

- Kotlin **2.4.21** is the user-confirmed baseline. Proceed with shared Android/iOS logic and Compose UI, fixing demonstrated failures rather than reopening speculative version compatibility work. Current versions and independent Compose/Material 3 versions live in the catalog.
- Use **Koin**, feature-owned definitions, typed constructor dependencies, and entry-scoped ViewModels. Native source sets supply database paths, HTTP engines, connectivity, and Android dynamic colors. Only `:shared` produces the iOS framework. Target `iosArm64` and `iosSimulatorArm64`, configured minimum iOS 15. Simulator support was added at the user's explicit request on 9 November; see the minimum-version follow-up below.
- **Remove accounts:** no email, password, login/logout, credential prompts, or session expiry. One device-local Room profile owns a random installation ID and the temporary display name **Josh**. Identity initialization is idempotent. The ID/name do not establish backend authorization.
- **Onboarding is deferred.** No welcome/name-entry UI in this change. The private ignored Todo records first-launch name entry and an introduction explaining features. Future work replaces the Josh seed deliberately.
- The user explicitly authorized a **one-time destructive reset** of retired account databases (versions 1–6) into device-profile schema 7. Old accounts, bookmarks, and cache are not imported. Android retires credential/session preferences and old saved navigation state. Preserve version-7 data and later schemas; do not extend this exception.
- Home, Watchlists, and Profile remain retained bottom tabs. Watchlists now supports local custom lists, management, search, and quick-add. Profile shows Josh and local bookmarks. The bottom bar remains visible in details. Switching tabs preserves history; reselection pops the active tab to its root. Back from another tab root returns Home's retained stack; Android Home root exits. NavKeys use an explicit cross-platform serializer registry. See [navigation](navigation.md).
- Shared Lists means two people adding to/reviewing lists on separate devices. Supabase Free remains a candidate; no provisioning, SDK, schema, sync, or backend implementation has been added. Agree identity/authorization, invites, membership, offline/conflict behavior, and two-device acceptance separately. Repositories depend on a vendor-neutral database API; provider-specific adapters sit behind that API and Koin bindings. See [public-source guidance](open-source.md).
- Treat the project as public portfolio source. Keep real credentials, local configuration, live data, and private `Notes/` ignored. Publish setup and placeholders. Preserve Human.md feedback; proposals need agreement before becoming rules.
- **Latest standing testing instruction:** never start, run, or interact with emulators/simulators, or execute connected/instrumented suites. Host tests/builds are allowed; ask for physical Android/iPhone observations. This supersedes the earlier automated-native exception recorded historically below.

## Implementation notes

- Library code/assets now live in `commonMain`/`composeResources`; platform integrations use `androidMain`/`iosMain`. Android host uses standard `app/src/main/kotlin`. Obsolete Android source copies and account-only tests were retired after replacement builds and retained test ports compiled; historical Room schemas remain.
- Room 2.8.5 uses bundled SQLite and generated platform constructors. Bookmarks are device-local. Catalogue/detail TTLs remain six/24 hours, cancellation propagates, and refresh locking is shared Kotlin. Ktor uses OkHttp on Android and Darwin on iOS; JSON tolerates unknown fields and uses 64-bit revenue/budget values.
- Shared conventions provide KMP, Compose, KSP, and opt-in Android device tests; runtime dependencies remain explicit. Database exposes dependencies needed by its public supertypes/DAO signatures. Five TestKit fixtures cover conventions and isolation. Hilt's unused convention was retired.
- Android's ignored TMDB constant is now `movie/internal/src/androidMain/kotlin/com/jcotters/internal/ApiConstants.kt`. Existing credentials were preserved without printing. CI generates only a placeholder at that path. iOS uses ignored `iosApp/Local.xcconfig`; start from its tracked example.
- `iosApp/Movers.xcodeproj` is tracked and ready to open. XcodeGen is only needed to regenerate it after changing `project.yml`. Configure an Apple team locally for your physical iPhone; no signing team or privileged credentials are tracked.
- `./scripts/verify.sh` runs conventions plus Android/shared host tests, lint, and debug packaging. `./scripts/verify-ios.sh` builds unsigned using the physical-device SDK, without installation or launch. See [testing](testing.md) for focused commands and report locations.

## Verification evidence

Evidence covers baseline `50a788b` plus this working tree, 8 November 2026. Some tasks reused up-to-date outputs; none of these builds establishes runtime acceptance.

- Final `./scripts/verify.sh` passed in 14 seconds: **69 shared host tests across 17 suites**, zero failures/errors/skips. Startup waiting/duplicate requests, retry, and cancellation coverage executed in the final run; other suites partly reused previous results. Build logic reports **5 tests**, zero failures/errors/skips; the final run reused those results. App lint/debug APK passed. App lint reports **30 warnings**, zero errors; existing native-library stripping/Room experimental notices are not suppressed.
- `./scripts/verify-ios.sh` passed with installed Xcode 26.3, iOS SDK 26.2, JDK 17, and the physical-device SDK. Current shared framework, Compose resources, SwiftUI host, and unsigned iOS app compile. No simulator, signing, installation, or launch. Native compilation earlier executed; the final invocation reused some framework outputs and executed 94 Gradle tasks.
- `./gradlew :app:assembleRelease :shared:assembleAndroidDeviceTest :movie:internal:assembleAndroidDeviceTest :tests:assembleDebug --console=plain` passed in one minute (197 executed tasks, 338 up-to-date). This covers minification/resource shrinking and retained integration-test/standalone APK compilation, **not test execution**.
- Two actual `:app:assembleDebug --configuration-cache --configuration-cache-problems=fail` builds passed; the second reused the configuration cache. Cache remains opt-in and this does not establish compatibility for every task.
- All 41 local documentation links resolved; Human.md remains 199 words with feedback preserved. Shell syntax (including both CI shell blocks), workflow/project YAML, shared resource XML, Room schema JSON, Xcode project/Info.plist parsing, and `git diff --check` passed. No Android/JVM/Hilt imports remain in common Kotlin source. Credentials, build artifacts, and private Notes remain ignored. Actionlint/remote CI were not run.

## Next action

1. First verify the implemented local list/search journeys using [the feature checklist](navigation.md#physical-device-acceptance), then verify Android and iPhone baseline behavior with the [physical-device checklist](multiplatform.md#verification-and-physical-device-acceptance): loading/paging, details/retry, bookmark/Profile observation, Josh, retained tabs/back, relaunch persistence, offline/reconnect, themes/insets. Use disposable legacy Android data to confirm the authorized one-time reset. Supply device/OS versions and sanitized observations/screenshots/logs.
2. User runs compiled Android navigation/DAO/cache/reset integration tests on a physical device. Compilation alone is not a pass. Keep the private Navigation queue item pending until evidence arrives.
3. Reassess defects from local-list acceptance before planning sharing/collaboration. The private `Notes/Todo.md` is the ordered queue when present. Onboarding and Shared Lists require their own agreed feature plans; do not silently start either.
4. Remote Android GitHub CI, iOS CI, IDE sync, signing/distribution, and accessibility/runtime acceptance remain unverified. No commit, push, publication, repository-settings change, or backend provisioning was performed.

## Catalogue-to-details crash follow-up

The user reported a crash when tapping a catalogue movie. They explicitly authorized a **one-time, read-only exception** to inspect the already-running Fold emulator's logs. `adb logcat -b crash -d` identified `MissingResourceException` for `composeResources/com.jcotters.presentation.resources/drawable/bookmark_outline.xml`. No app launch, install, input, instrumented execution, or emulator start was performed; the standing no-emulator workflow remains unchanged.

The old APK contained only the composition module's Compose assets. Resource-owning feature/presentation libraries generated accessors but did not package their assets because Android resources were disabled. Enable `androidResources.enable` in `movers.multiplatform.compose` for every consumer. The shared Compose TestKit fixture now builds an AAR and inspects an actual drawable entry; it failed on the missing asset before the fix. See [the convention rationale](../../build-logic/README.md) and [packaging checks](testing.md).

`./scripts/verify.sh` passed after the fix: 69 app/shared tests and five convention tests, zero failures/errors/skips, with 30 lint warnings and no errors. Convention checks executed nine tasks; the app invocation executed 448 tasks and reused 24. Debug and shrunk release APK inspection confirmed all 26 expected Compose assets across the five resource-owning modules, including both bookmark icons. Release plus shared/movie integration-test APK compilation passed (57 seconds, 167 tasks executed, 353 up-to-date); tests were not executed on a device. `./scripts/verify-ios.sh` also passed unsigned with the physical-device SDK (30-second Gradle phase, 94 tasks executed, 45 up-to-date). Documentation links and `git diff --check` passed. These results cover baseline `50a788b` plus the current uncommitted source; the user's staged changes were preserved.

After rebuilding/reinstalling, the user must reopen a catalogue movie, toggle its bookmark, and check Profile; runtime recovery is not established by host checks.

## Host UI regression gate and vector crash

The user authorized an emulator launch/open-details/Back debugging loop. One cold launch and catalogue tap reproduced a second crash: `IllegalArgumentException: Invalid color value @android:color/white`. Android CLI installed its layout-inspection helper; no app APK was installed and no connected test suite was executed. The user then requested automated UI testing that respects the no-device policy. Emulator interaction stopped at that point. The standing no-emulator/simulator policy remains in force.

Use **host Robolectric Compose UI tests** for this regression. The new opt-in `movers.multiplatform.ui-testing` convention supplies Robolectric 4.17, Compose testing, Android resources, and Java 17 access. It affects host test dependencies only. `CatalogueNavigationUiTest` uses the actual shared app/Koin graph, production screens/ViewModels/repositories/Room schema, an in-memory database with the host Android SQLite driver, and HTTP fixtures. Production bundled-SQLite and native OS differences remain separate acceptance work.

The Fold-sized host test reproduced the exact vector-color exception before the fix, without ADB/device/emulator use. Replaced Android color references with equivalent literal white in all 12 affected shared vectors. The host UI suite then passed all three cases: repeated details/Back and bookmark/Profile navigation at phone/Fold sizes, plus saved-state restoration and Back. State-based waits cover asynchronous persistence and bookmark readiness.

**Accepted completion rule:** maintain a relevant passing host UI regression for shared UI/navigation changes before marking work complete. `verify.sh` and Android CI already run `testAndroidHostTest`, so this is part of the normal verification gate, not a manual optional emulator step. Physical-device acceptance still covers OS-specific behavior and iOS. Human.md, root instructions, and the testing/build guides preserve the rule.

Final verification against committed source `c21bd56`, 9 November 2026:

- `./scripts/verify.sh` passed: **72 host unit/UI tests across 18 suites**, including all three `CatalogueNavigationUiTest` cases, plus **five convention tests**; zero failures/errors/skips. The app phase took 52 seconds (458 tasks executed, 24 up-to-date); convention checks executed nine tasks and reused four. Lint/debug packaging passed with 30 warnings and no lint errors. All 26 expected Compose assets are present in the debug APK.
- `./scripts/verify-ios.sh` passed unsigned with the physical-device SDK (32-second Gradle phase, 97 tasks executed, 42 up-to-date). No device/simulator was used.
- Release and integration-test APK packaging passed (1 minute 19 seconds, 167 tasks executed, 353 up-to-date); the release APK contains all 26 expected Compose assets. Device-test execution remains pending; host UI tests above were executed.
- All 44 local documentation links resolved, Human.md is 208 words, shell syntax and `git diff --check` passed. The user committed the code and import correction; only this refreshed handoff is uncommitted at review time. No agent commit or push was performed.

## Requested iOS Simulator validation

The user explicitly requested building, launching, and validating on their already-open **iPhone 14 Plus** simulator. Later they approved automated Android/iOS smoke tests after every change set; that current standing rule is recorded above and in root instructions. It preserves the host UI-test gate and limits unrelated/manual device interaction.

Added `iosSimulatorArm64` to the shared library convention, a matching static framework in `:shared`, and Room KSP processing for that target. Existing `iosMain` implementations are reused. The open simulator runs iOS 26.2 on Apple silicon. XcodeBuildMCP uses session-only settings; no machine-local simulator identifiers are committed. Prepared ignored `iosApp/Local.xcconfig` from the existing local Android TMDB token without printing it or replacing existing configuration.

The first Simulator build/run succeeded in 227 seconds. Home loaded live movie data/posters, and details displayed metadata and bookmark controls. The user also interacted manually during exploration, so a repeatable native test was added to remove ambiguity.

## Shared Android/iOS native smoke test

Added `.maestro/catalogue-navigation.yaml` and `scripts/verify-ui.sh`. One flow uses the same app ID and visible actions on both platforms: launch, open details/return three times, visit Watchlists/Profile, return Home, then relaunch and assert the catalogue. The final relaunch is the last assertion, not a second test; the app intentionally remains open afterward. It preserves app data and bookmarks, and needs live TMDB configuration/network or a populated catalogue cache. The hermetic host UI test remains the routine CI/completion gate.

The initial iOS native run failed because edge-swipe return did not reach Home. Added an accessible Back button to the shared detail header, wired through the feature's existing Navigator callback. The same smoke flow now uses that visible Back action on both OSes. Host tests cover UI Back at phone size/restoration and system Back at Fold size. The iOS edge gesture remains an open follow-up; do not report it as verified.

**Executed iOS result:** Maestro CLI 2.12.1 passed **1/1 flow in 24 seconds** on iPhone 14 Plus / iOS 26.2, including three detail/Back cycles, tabs, and relaunch. JUnit output: `build/ui-smoke/ios/report.xml`; command traces/screenshots are in its ignored `artifacts/` directory. The app was rebuilt/installed/launched with XcodeBuildMCP after the Back fix (32.7 seconds). The official Maestro distribution was checksum-verified and extracted only in temporary storage; no global installation, cloud execution/upload, or app-data reset. Analytics are disabled by the wrapper.

**Executed Android result:** the identical Maestro flow passed **1/1 in 38 seconds** on the already-running Fold (`sdk_gphone16k_arm64`). Installed the current debug APK in place without clearing data before running it. JUnit output: `build/ui-smoke/android/report.xml`. Both platforms therefore executed the shared definition; this is not inferred from compatibility or compilation.

The wrapper requires explicit platform/device arguments, checks that the selected target is already running/connected, and propagates failures. **Nine host-only stub checks** passed for arguments, missing prerequisites, offline targets, correct invocation from outside the repo, and exit-code propagation. Maestro's syntax check passed. At that point the user agreed to routine automated native smoke runs. The latest standing no-emulator/simulator instruction supersedes that exception; current work uses host gates and leaves physical-device acceptance to the user. Add relevant tests when new features such as Watchlists are implemented. Native GitHub CI jobs have not been added/run; the current CI job executes the host gate.

After the Back change, `./scripts/verify.sh` passed again: **72 host tests plus five convention tests**, zero failures/errors/skips; lint/debug build passed (26-second app phase, 73 executed tasks, 409 up-to-date). `verify-ios.sh` passed unsigned for physical iOS (18-second Gradle phase, 65 executed tasks, 93 up-to-date). Final Android release and retained integration-test APK compilation passed (69 seconds, 167 tasks executed, 353 up-to-date); those integration tests were not executed. Debug and shrunk release APKs contain all **27** expected Compose assets, including the Back icon. Source is `c21bd56` plus the current Simulator/Back/smoke-workflow changes. User staging was preserved; no agent commit/push.

**Compatibility follow-up:** the Simulator linker warns that a bundled ICU object was built for iOS Simulator 18.5 while the configured deployment target is 15.0. Runtime validation here covers iOS 26.2 only. Review the minimum supported iOS version before advertising older-OS compatibility; no dependency/minimum-version change or warning suppression was introduced.

Final documentation checks: 46 local links resolve, Human.md remains concise at 211 words with feedback preserved, all verification scripts pass shell syntax checks, and `git diff --check` passes. Local iOS credentials and all native test artifacts remain ignored.

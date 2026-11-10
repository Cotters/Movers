# Verification

## Current feature-phase policy

Use source/diff/static checks during iteration; do not run routine builds or compilation-backed tests. Run Gradle/Xcode verification only when explicitly requested, or when a concrete need for a substantial change/release warrants it. Explain the need first and choose the smallest useful scope. Ordinary edits have no automatic final build gate. Update existing assertions for intentional behavior changes; run the existing suite when verification is requested/necessary and report anything deferred. Do not add new test cases, especially UI tests, unless the user explicitly requests them. Defer broader coverage work until the post-feature review recorded in private `Notes/Todo.md`. Keep existing checks passing; investigate failures rather than disabling tests or weakening assertions. The guidance below describes the retained infrastructure and future coverage choices, not a requirement to expand coverage during this phase.

## Verification by change

| Change | Iteration | Final evidence |
| --- | --- | --- |
| Documentation/instructions only | Check the changed wording against current source/policy. | `./scripts/verify-docs.sh` and `git diff --check`; no app rebuild. |
| Documentation checker / CI wiring | Shell syntax, run the checker, inspect workflow YAML and relevant shell blocks. | Same checks locally; remote CI remains pending. App checks are needed only if their behavior/configuration changes. |
| Android app behavior | Inspect source, call sites, expectations and diff. | When requested/necessary: relevant existing host checks, or `./scripts/verify.sh` for broad verification; physical acceptance is separate. |
| Shared/iOS behavior | Inspect common/platform boundaries and existing expectations. | When requested/necessary: affected host checks and/or `./scripts/verify-ios.sh`; report their different coverage. |
| Schema, resources, conventions or targets | Inspect affected schema/migration SQL, assets and configuration. | Explain any necessary code generation/build check before running it; choose affected targets/checks only. Otherwise defer generated outputs to the next requested build. See [shared-code checks](multiplatform.md#changes-to-shared-code). |

These commands are available verification tools, not an instruction to build after each edit. Run requested/necessary gates once the relevant edits settle. Repeat only when a failure, new evidence or a change invalidates the previous result; do not repeat a successful cached build merely to produce fresh output. During a long check, independent reads/docs work can continue, but avoid editing its tested inputs.

**One build owner per checkout:** do not overlap Gradle invocations or Xcode builds that invoke Gradle against the same outputs. Wait for a running command's completion before reusing its log/report path. Use a distinct ignored log path, for example `build/verification/<task>-<run>/host.log`, and retain the command/exit status. A filtered test run overwrites that module's JUnit report: counts from it cannot be combined with an older full-suite claim.

Record the source checkpoint (HEAD and relevant uncommitted changes), command, run scope, exit/result, executed versus cached work and remaining limits in the handoff. A user commit during a run requires a content check, not automatic discard of all evidence. Compile-only, `NO-SOURCE`, skipped and cached results are different observations. Never infer an iOS/runtime pass from an Android host result.

## Documentation check

`./scripts/verify-docs.sh` requires Bash and Python 3, works from any current directory, and checks relative link targets in AGENTS.md, build-logic/README.md and workflow documents/history. It rejects missing targets and absolute/outside-repository local links. It uses no network, credentials, Gradle or device. External URLs and heading anchors still need review; this is not a semantic documentation or citation audit.

The existing Android CI job runs it immediately after checkout. This adds a deterministic documentation check without adding test cases or a new framework; it does not change the Android job's existing trigger/build policy.

## Prerequisites

- Bash, an executable Gradle wrapper, and **JDK 17**. Set `JAVA_HOME` if the shell's Java differs from Android Studio's Gradle JDK.
- Android SDK Platform 37.0 (`platforms;android-37.0`) and Build Tools 36.0.0. Current configuration compiles against API 37, targets API 36 in the app, and supports API 28 onward. Source of truth: `gradle/libs.versions.toml`, module Gradle files, and `gradle/wrapper/gradle-wrapper.properties`.
- An SDK location in `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or ignored `local.properties` (`sdk.dir=...`). Environment variables should point to the same SDK if both are set.
- Dependencies in the Gradle cache or network access to the configured repositories. Current wrapper: Gradle 9.5.0; AGP: 9.3.3. [AGP compatibility reference](https://developer.android.com/build/releases/agp-9-3-0-release-notes).
- An ignored `movie/internal/src/androidMain/kotlin/com/jcotters/internal/ApiConstants.kt` for the existing movie module. Never commit or print real credentials.

For host verification without a TMDB account, create that file locally with:

```kotlin
package com.jcotters.internal

internal object ApiConstants {
    const val API_KEY = "CI_PLACEHOLDER_DO_NOT_USE"
}
```

Preserve any existing file. A placeholder permits compilation and mocked host tests, but cannot load live TMDB data. A person testing movie loading on a physical device needs their own TMDB API read-access token in this ignored constant. The verification script never creates or replaces credentials.

## Full host check

From the repository root:

```sh
./scripts/verify.sh
```

The script takes no arguments, works from any current directory, checks basic prerequisites, then executes:

```sh
./gradlew --no-daemon --console=plain -p build-logic :convention:check
./gradlew --no-daemon --console=plain --continue testDebugUnitTest testAndroidHostTest lintDebug :app:assembleDebug
```

The first invocation checks convention-plugin validation and Gradle TestKit fixtures; a failure stops verification. Gradle's task selector then runs matching unit-test/lint tasks across participating app modules. Shared modules use `testAndroidHostTest`; retained JVM tests and `commonTest` cases run on that host target. `--continue` collects independent task failures and still returns a failure status. The script preserves the failing Gradle invocation's exit code; prerequisite errors return 2 and unsupported arguments return 64. It does not install tools, contact a backend for tests, deploy an APK, or start a device. Dependency resolution can access artifact repositories.

## Focused verification

The following commands compile code; use them only under the requested/necessary build policy above. Use the existing JUnit 4, MockK, and `kotlinx-coroutines-test` stack. Examples:

```sh
./gradlew :features:details:testAndroidHostTest
./gradlew :profile:testAndroidHostTest --tests '*LocalDeviceProfileRepositoryTest'
./gradlew :database:testAndroidHostTest
./gradlew :app:assembleDebug
```

Choose tests for the guarantee being changed:

| Change | Useful verification |
| --- | --- |
| ViewModel/use-case decisions | Observable state/results, repeated submissions, delayed completion, failure/retry, cancellation, and account changes as relevant. Use controlled coroutine scheduling and clocks. |
| Repository/cache behavior | Preserve saved data on failed refresh, idempotent writes, concurrent callers, freshness boundaries, mapping, and cancellation. Mocked DAO tests cannot prove actual Room behavior. |
| Room schema/query | Export and review schemas; preserve device identity, lists, movie data and constraints outside the explicitly authorized legacy reset. Bookmarks are intentionally retired by 9→10. The existing host migration/list-storage case targets populated 7/8/9→10 migration, relationships, idempotence, and atomic rollback with Android SQLite. Production bundled-SQLite and legacy-reset instrumented checks await user execution on a physical device. |
| Compose/navigation | Run existing host Compose UI regressions; update expectations for intentional changes. Defer new cases during the feature phase. Check real screens/resources and user actions, not only substitute navigation entries. Physical acceptance still covers OS behavior, accessibility, and rendering differences. |
| Workflow/scripts/CI | Check shell syntax and prerequisite/failure handling, validate workflow YAML, and run the same script used by CI. Remote execution remains pending until GitHub runs it. |
| Convention plugins | Run `./gradlew -p build-logic :convention:check`; validate the full task graph with `./gradlew build --dry-run`. Check debug/release and test-APK compilation for plugin/code-generation changes. TestKit fixtures never start devices. |

For Compose resource or packaging changes, inspect the built APK's `assets/composeResources/` entries for each resource-owning module. Compilation and generated accessors do not prove the resource files are packaged. The shared Compose convention's TestKit fixture checks a drawable inside the built AAR.

## Host UI regression gate

For layout work, use bounded, state-only Compose previews alongside the existing host suite. Movie-detail previews include loading, partial/error data, long titles with large text and a wide viewport; image components use static inspection-mode placeholders without initializing the live image loader. `ComposeViewAdapter` must resolve on the feature's Android runtime classpath (see [conventions](../../build-logic/README.md)). Ask the user to sync/refresh and report actual IDE rendering; previews/builds do not replace physical-device layout acceptance.

The app uses JUnit 4, MockK, Koin, and Compose. `movers.multiplatform.ui-testing` adds Robolectric only to opted-in Android host tests, with Android resources and Compose test APIs. No emulator, simulator, ADB, installation, or device interaction is involved. Run the UI regression directly or through the complete host script:

```sh
./gradlew :shared:testAndroidHostTest --tests '*CatalogueNavigationUiTest'
./scripts/verify.sh
```

`WatchlistsUiTest` covers list management, duplicate-name warnings, immediate multi-list check/uncheck and restoration, editable saved membership, inline list creation/automatic addition, consecutive contextual search add/remove, source retention, and search failure/retry/empty states. `WatchlistPersistenceTest` opens populated exported version-7/8/9 schemas, migrates/reopens version 10, and checks retired bookmarks plus preserved profile/lists/enrichment/shared cache; it also checks real adapter idempotence and transaction rollback.

`CatalogueNavigationUiTest` opens the real shared app, enters movie details and uses Back repeatedly, checks Profile and returns to retained Home details, and restores saved navigation. It runs at phone and Fold-sized configurations. HTTP responses are fixed fixtures; movie data does not require credentials or a live service. It uses the production Koin definitions, ViewModels, repositories, and Room schema, with an in-memory database and Robolectric's Android SQLite driver instead of production bundled SQLite. Those driver/native-platform differences still require separate integration acceptance.

The tests use Robolectric 4.17, API 35, and native graphics. The watchlist suite drains the host main queue in an outer rule after Compose disposes its content; doing so in @After is too early and can leave Paging’s process-wide dispatcher stalled for later cases. Keep UI tests deterministic and use semantics/state-based waits. When the suite has not run under the build policy, report shared UI/navigation changes as unverified; do not infer a pass. New-case expansion is deferred during the feature phase. `testAndroidHostTest` already runs in `verify.sh` and Android CI; failures propagate to a failing verification result. Host simulation does not prove iOS behavior or replace physical-device acceptance.

References: [Robolectric setup](https://robolectric.org/getting-started/) and [Android KMP host-test resources](https://developer.android.com/kotlin/multiplatform/plugin#configure-tests).

## Shared native smoke flow

[The catalogue flow](../../.maestro/catalogue-navigation.yaml) and [watchlist flow](../../.maestro/watchlists.yaml) use the same app ID and visible actions on Android and iOS. The catalogue flow opens details/Back repeatedly, visits tabs, and relaunches. The watchlist flow creates uniquely named lists, adds a catalogue movie to both, searches/adds from a list, renames/removes, checks relaunch persistence, then deletes only its own test lists. Neither flow clears app data. If interrupted, remove only lists with that run's Smoke timestamp prefix. Unlike the host fixture test, this smoke flow needs a configured TMDB token/network or a populated catalogue cache.

Install [Maestro CLI 2.12.1](https://github.com/mobile-dev-inc/maestro/releases/tag/cli-2.12.1) with Java 17, or point `MAESTRO_BIN` at its executable. These are retained user-run tools. **The latest standing instruction forbids agents from running or interacting with emulators/simulators**, superseding earlier smoke-test exceptions. Agents may run requested/necessary host verification and compile apps; people perform native acceptance on physical devices. The retained iOS Maestro wrapper targets a simulator, so leave it unexecuted under that instruction. For a separately authorized/user-run native check, build/install the current app first:

```sh
./scripts/verify-ui.sh ios <simulator-id>
./scripts/verify-ui.sh android <device-id>
```

The wrapper rejects an absent/offline target and does not build, install the app, or start devices. Maestro installs its test driver as needed. Keep manual interaction idle during the run. JUnit output is `build/ui-smoke/<platform>/report.xml`; local command traces/screenshots are under `artifacts/`. All outputs are ignored. Failure propagates to a nonzero exit code; analytics are disabled. The wrapper does not invoke cloud services or upload test artifacts.

This native flow is separate from `verify.sh` and the existing device-free CI job; native results remain pending until supplied by the user. A shared flow definition does not establish a pass on both platforms: record each actual platform run separately. Do not start or interact with targets to obtain a result; request physical-device observations. Defer new host/native cases to the post-feature review unless requested. Native CI jobs remain future setup work. The flow does not replace physical-device acceptance or verify the iOS edge gesture. See [Maestro's cross-platform model](https://docs.maestro.dev/get-started/how-maestro-works).

Do not add a new test framework, screenshot system, or broad coverage target merely to change documentation. Test user-visible guarantees rather than private implementation calls. `NO-SOURCE`, empty test scaffolding, compilation, and skipped tests are not passing behavior coverage.

## Reports and evidence

- Shared unit XML: `<module>/build/test-results/testAndroidHostTest/TEST-*.xml`; Android-only hosts use `testDebugUnitTest` when they contain tests.
- Shared unit HTML: `<module>/build/reports/tests/testAndroidHostTest/index.html`. Do not count stale pre-migration report directories as current evidence.
- Build-logic tests: `build-logic/convention/build/test-results/test/` and `build-logic/convention/build/reports/tests/test/`.
- Lint: `<module>/build/reports/lint-results-debug.html` and `.xml`/`.sarif` where generated.
- Debug APK: `app/build/outputs/apk/debug/`; building it does not verify runtime behavior.

Record the command, date, source revision/working-tree state, result, and remaining limits in the handoff. Distinguish executed tests from `UP-TO-DATE`/`FROM-CACHE` tasks. Existing failures belong in follow-up notes; do not hide them with broad ignores, test disabling, or `continue-on-error`.

## CI

`.github/workflows/android.yml` runs on pull requests, pushes to `main`, and manual dispatch. Ubuntu 24.04 with Temurin 17 installs the required SDK packages, creates the ignored placeholder constant on the disposable runner, and invokes `./scripts/verify.sh`. It needs no TMDB/Supabase credentials. Java setup caches Gradle dependencies; report artifacts are retained for seven days, including available reports after failure. APKs are not published.

The workflow does not change repository branch protection or run deployments. A local passing check and workflow validation do not establish a successful GitHub run.

## Physical-device handoff

Never start, run, or interact with emulators/simulators, and do not execute connected/instrumented suites. Physical-device acceptance remains user-owned; earlier automated-device exceptions are historical. Include the build/revision, exact steps, expected result, and required observations; request device/OS versions and sanitized logs or screenshots when relevant.

Existing pending areas include Room reset/DAO integration, Compose flows, relaunch/background/foreground behavior, and real connectivity handovers. For storage changes the user should run `DeviceDatabaseResetTest`, the DAO tests, and `PopularMoviesRemoteMediatorTest` on a physical Android device; for UI changes provide specific changed journeys. No device check is required just to read these workflow documents. Keep runtime verification pending until results arrive.

## Shared Android/iOS verification

When shared native verification is requested/necessary, run `./scripts/verify-ios.sh` on macOS with full Xcode selected and JDK 17 available. It builds the actual iOS app unsigned, using only the physical-device SDK and a generic iOS destination. It never installs or launches an app. Open the tracked Xcode project and configure your signing team locally for physical-iPhone testing. See [setup and acceptance](multiplatform.md).

Before release/archive handoff, also run `./scripts/verify-ios.sh --archive`. This uses Release configuration and produces the unsigned `build/iosArchives/Movers.xcarchive`, without installation, export or upload. Debug verification does not exercise Kotlin/Native's whole-program Release optimization. The project allows a 4 GB JVM heap for Gradle/Kotlin/Native through `org.gradle.jvmargs`; a Java heap-space failure in `linkReleaseFrameworkIosArm64` concerns the build JVM, not app runtime memory. See [Kotlin's heap guidance](https://kotlinlang.org/docs/native-improving-compilation-time.html#increase-gradle-heap-size). Preserve optimizations and inspect the failing phase before changing compiler flags or dependencies. A successful unsigned archive does not validate provisioning or App Store acceptance.

Keep supplied archive logs local: Xcode can export live configuration and personal signing settings into them. Root `archive_log.txt` is ignored; use ignored `build/verification/` for new build logs.

The current Android integration tests compile without running them:

```sh
./gradlew :shared:assembleAndroidDeviceTest :movie:internal:assembleAndroidDeviceTest
./gradlew :app:assembleRelease
```

The shared test APK contains navigation/store/restoration, movie-screen, DAO, and reset/persistence tests. Movie internal retains its real Room/mediator integration tests. Login/session and legacy preservation scenarios were retired with the removed account feature; the reset test checks version-6 data is discarded once and current device-profile data survives reopening. The existing host migration case targets populated versions 7, 8 and 9 through version 10, including profile confirmation/color, list membership, shared snapshots/operations/activity and deliberate bookmark-table removal. Read the handoff for actual execution status.

For configuration-cache checks, run `./gradlew :app:assembleDebug --configuration-cache --configuration-cache-problems=fail` twice and require reuse. Cache compatibility is task-specific and remains opt-in globally.

The [navigation checklist](navigation.md#physical-device-acceptance) and [multiplatform acceptance](multiplatform.md#verification-and-physical-device-acceptance) remain user-owned. Building Android test APKs, a minified release, and an unsigned iOS app does not establish runtime acceptance. Remote GitHub execution and iOS CI remain unverified until actually run.

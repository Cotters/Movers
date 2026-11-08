# Verification

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

## Focused iteration

Use the existing JUnit 4, MockK, and `kotlinx-coroutines-test` stack. Examples:

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
| Room schema/query | Export and review schemas; preserve device identity, bookmarks, and constraints outside the explicitly authorized legacy reset. DAO/cache integration and the reset are instrumented checks and await user execution on a physical device. |
| Compose/navigation | Add/update and pass a relevant host Compose UI regression before completion. Check real screens/resources and user actions, not only substitute navigation entries. Physical acceptance still covers OS behavior, accessibility, and rendering differences. |
| Workflow/scripts/CI | Check shell syntax and prerequisite/failure handling, validate workflow YAML, and run the same script used by CI. Remote execution remains pending until GitHub runs it. |
| Convention plugins | Run `./gradlew -p build-logic :convention:check`; validate the full task graph with `./gradlew build --dry-run`. Check debug/release and test-APK compilation for plugin/code-generation changes. TestKit fixtures never start devices. |

For Compose resource or packaging changes, inspect the built APK's `assets/composeResources/` entries for each resource-owning module. Compilation and generated accessors do not prove the resource files are packaged. The shared Compose convention's TestKit fixture checks a drawable inside the built AAR.

## Host UI regression gate

The app uses JUnit 4, MockK, Koin, and Compose. `movers.multiplatform.ui-testing` adds Robolectric only to opted-in Android host tests, with Android resources and Compose test APIs. No emulator, simulator, ADB, installation, or device interaction is involved. Run the UI regression directly or through the complete host script:

```sh
./gradlew :shared:testAndroidHostTest --tests '*CatalogueNavigationUiTest'
./scripts/verify.sh
```

`CatalogueNavigationUiTest` opens the real shared app, enters movie details and uses Back repeatedly, bookmarks via Profile, and restores saved navigation. It runs at phone and Fold-sized configurations. HTTP responses are fixed fixtures; movie data does not require credentials or a live service. It uses the production Koin definitions, ViewModels, repositories, and Room schema, with an in-memory database and Robolectric's Android SQLite driver instead of production bundled SQLite. Those driver/native-platform differences still require separate integration acceptance.

The test uses Robolectric 4.17, API 35, and native graphics. Keep UI tests deterministic and use semantics/state-based waits. A relevant UI test must pass before marking shared UI/navigation work complete. `testAndroidHostTest` already runs in `verify.sh` and Android CI; failures propagate to a failing verification result. Host simulation does not prove iOS behavior or replace physical-device acceptance.

References: [Robolectric setup](https://robolectric.org/getting-started/) and [Android KMP host-test resources](https://developer.android.com/kotlin/multiplatform/plugin#configure-tests).

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

Agents never start, run, or interact with an emulator or simulator, and do not execute connected/instrumented tests. Ask the user to run applicable checks on their own physical device. Include the build/revision, exact steps, expected result, and required observations; request Android version/device model and sanitized logs or screenshots when relevant.

Existing pending areas include Room reset/DAO integration, Compose flows, relaunch/background/foreground behavior, and real connectivity handovers. For storage changes the user should run `DeviceDatabaseResetTest`, the DAO tests, and `PopularMoviesRemoteMediatorTest` on a physical Android device; for UI changes provide specific changed journeys. No device check is required just to read these workflow documents. Keep runtime verification pending until results arrive.

## Shared Android/iOS verification

Run `./scripts/verify-ios.sh` on macOS with full Xcode selected and JDK 17 available. It builds the actual iOS app unsigned, using only the physical-device SDK and a generic iOS destination. It never installs or launches an app. Open the tracked Xcode project and configure your signing team locally for physical-iPhone testing. See [setup and acceptance](multiplatform.md).

The current Android integration tests compile without running them:

```sh
./gradlew :shared:assembleAndroidDeviceTest :movie:internal:assembleAndroidDeviceTest
./gradlew :app:assembleRelease
```

The shared test APK contains navigation/store/restoration, movie-screen, DAO, and reset/persistence tests. Movie internal retains its real Room/mediator integration tests. Login/session and legacy preservation scenarios were retired with the removed account feature; the new reset test checks version-6 data is discarded once and new version-7 profile data survives reopening.

For configuration-cache checks, run `./gradlew :app:assembleDebug --configuration-cache --configuration-cache-problems=fail` twice and require reuse. Cache compatibility is task-specific and remains opt-in globally.

The [navigation checklist](navigation.md#physical-device-acceptance) and [multiplatform acceptance](multiplatform.md#verification-and-physical-device-acceptance) remain user-owned. Building Android test APKs, a minified release, and an unsigned iOS app does not establish runtime acceptance. Remote GitHub execution and iOS CI remain unverified until actually run.

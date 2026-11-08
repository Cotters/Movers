# Verification

## Prerequisites

- Bash, an executable Gradle wrapper, and **JDK 17**. Set `JAVA_HOME` if the shell's Java differs from Android Studio's Gradle JDK.
- Android SDK Platform 37.0 (`platforms;android-37.0`) and Build Tools 36.0.0. Current configuration compiles against API 37, targets API 36 in the app, and supports API 28 onward. Source of truth: `gradle/libs.versions.toml`, module Gradle files, and `gradle/wrapper/gradle-wrapper.properties`.
- An SDK location in `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or ignored `local.properties` (`sdk.dir=...`). Environment variables should point to the same SDK if both are set.
- Dependencies in the Gradle cache or network access to the configured repositories. Current wrapper: Gradle 9.5.0; AGP: 9.3.3. [AGP compatibility reference](https://developer.android.com/build/releases/agp-9-3-0-release-notes).
- An ignored `movie/internal/src/main/java/com/jcotters/internal/ApiConstants.kt` for the existing movie module. Never commit or print real credentials.

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
./gradlew --no-daemon --console=plain --continue testDebugUnitTest lintDebug :app:assembleDebug
```

Gradle's task selector runs matching unit-test/lint tasks across participating modules. `--continue` collects independent task failures and still returns a failure status. The script preserves Gradle's exit code; prerequisite errors return 2 and unsupported arguments return 64. It does not install tools, contact a backend for tests, deploy an APK, or start a device. Dependency resolution can access artifact repositories.

## Focused iteration

Use the existing JUnit 4, MockK, and `kotlinx-coroutines-test` stack. Examples:

```sh
./gradlew :features:details:testDebugUnitTest
./gradlew :auth:testDebugUnitTest --tests '*UserSessionShould'
./gradlew :database:testDebugUnitTest :database:lintDebug
./gradlew :app:assembleDebug
```

Choose tests for the guarantee being changed:

| Change | Useful verification |
| --- | --- |
| ViewModel/use-case decisions | Observable state/results, repeated submissions, delayed completion, failure/retry, cancellation, and account changes as relevant. Use controlled coroutine scheduling and clocks. |
| Repository/cache behavior | Preserve saved data on failed refresh, idempotent writes, concurrent callers, freshness boundaries, mapping, and cancellation. Mocked DAO tests cannot prove actual Room behavior. |
| Room schema/query | Export and review schemas; preserve existing accounts/bookmarks and constraints. Existing DAO/migration tests are instrumented and await user execution on a physical device. |
| Compose/navigation | Compile, inspect previews where useful, and review state/semantics. User verifies visible behavior, back-stack/lifecycle transitions, large fonts, and accessibility on a physical device. |
| Workflow/scripts/CI | Check shell syntax and prerequisite/failure handling, validate workflow YAML, and run the same script used by CI. Remote execution remains pending until GitHub runs it. |

Do not add a new test framework, screenshot system, or broad coverage target merely to change documentation. Test user-visible guarantees rather than private implementation calls. `NO-SOURCE`, empty test scaffolding, compilation, and skipped tests are not passing behavior coverage.

## Reports and evidence

- Unit XML: `<module>/build/test-results/testDebugUnitTest/TEST-*.xml`.
- Unit HTML: `<module>/build/reports/tests/testDebugUnitTest/index.html`.
- Lint: `<module>/build/reports/lint-results-debug.html` and `.xml`/`.sarif` where generated.
- Debug APK: `app/build/outputs/apk/debug/`; building it does not verify runtime behavior.

Record the command, date, source revision/working-tree state, result, and remaining limits in the handoff. Distinguish executed tests from `UP-TO-DATE`/`FROM-CACHE` tasks. Existing failures belong in follow-up notes; do not hide them with broad ignores, test disabling, or `continue-on-error`.

## CI

`.github/workflows/android.yml` runs on pull requests, pushes to `main`, and manual dispatch. Ubuntu 24.04 with Temurin 17 installs the required SDK packages, creates the ignored placeholder constant on the disposable runner, and invokes `./scripts/verify.sh`. It needs no TMDB/Supabase credentials. Java setup caches Gradle dependencies; report artifacts are retained for seven days, including available reports after failure. APKs are not published.

The workflow does not change repository branch protection or run deployments. A local passing check and workflow validation do not establish a successful GitHub run.

## Physical-device handoff

Agents never start, run, or interact with an emulator or simulator, and do not execute connected/instrumented tests. Ask the user to run applicable checks on their own physical device. Include the build/revision, exact steps, expected result, and required observations; request Android version/device model and sanitized logs or screenshots when relevant.

Existing pending areas include Room migration/DAO integration, Compose flows, session expiry/background/foreground behavior, and real connectivity handovers. For migrations the user should run the existing `DatabaseMigrationTest` on their physical device with the test runner; for UI changes provide specific changed journeys. No device check is required just to read these workflow documents. Keep runtime verification pending until results arrive.

## Nav 3 verification

Navigation policy and cross-module key serialization are covered by `NavigationStateShould`; Auth tests cover UI-supplied credentials, one credential request per entry, and clearing passwords on success. Watchlists tests cover session initialization and login/logout states. Run focused checks with:

```sh
./gradlew :app:testDebugUnitTest :auth:testDebugUnitTest :features:watchlists:testDebugUnitTest
```

`MoversNavigationShellTest` covers independent detail ViewModel stores, Auth-to-Profile replacement, expiry cleanup, and saved navigation restoration. It is instrumented: agents compile it but leave execution to the user on a physical device. Build it and affected existing tests without running them:

```sh
./gradlew :app:assembleDebugAndroidTest :auth:assembleDebugAndroidTest :features:catalogue:assembleDebugAndroidTest :features:details:assembleDebugAndroidTest
./gradlew :app:assembleRelease
```

Use the [physical-device navigation checklist](navigation.md#physical-device-acceptance) and record the outcome in the handoff. Compiled test APKs and a minified build are not proof of runtime behavior.

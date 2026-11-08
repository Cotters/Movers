# Build conventions

The included `build-logic` build provides small, additive plugins. SDK, compiler, and library versions come from the root catalog. Android hosts use AGP built-in Kotlin/new DSL; shared modules use Kotlin Multiplatform with Google's Android KMP library plugin. Java/Android bytecode targets Java 17.

| Plugin | Responsibility | Consumers |
| --- | --- | --- |
| `movers.android.application` | Android application defaults | `:app` |
| `movers.android.compose` | Android Compose compiler/BOM/tooling | `:app`, `:tests` |
| `movers.android.test` | Standalone Android test defaults | `:tests` |
| `movers.android.library` | Android-only library defaults | Available for platform-only libraries |
| `movers.android.unit-testing` | Android JUnit/MockK/coroutine-test setup | `:app` and build-logic fixtures |
| `movers.multiplatform.library` | Android + physical-iOS targets, SDK/JVM defaults, common/host test infrastructure | Shared libraries and contracts |
| `movers.multiplatform.compose` | Shared Compose UI, resources, previews | `:shared`, presentation, catalogue, details, watchlists, profile |
| `movers.multiplatform.ksp` | Opt-in KSP for shared code | `:database` |
| `movers.multiplatform.device-testing` | Android resource/device-test setup and integration-test dependencies | `:shared`, `:movie:internal` |
| `movers.multiplatform.ui-testing` | Opt-in host Compose UI tests with Robolectric, Android resources, and Java 17 access | `:shared` |
| `movers.kotlin.serialization` | Serialization compiler plugin | Key contracts, movie transport, shared composition |

For a new module, add its settings include, select a base/conventions, and declare its namespace and dependencies locally. Shared conventions do not inject Koin, navigation, networking, or storage. Runtime dependencies remain explicit. Koin definitions belong to their owning features/data modules; the app composes those modules. Hilt and its convention were retired with account-based Android wiring.

Keep application identity/version, release rules, Room schema paths/processors, and platform mock behavior local. Avoid root `subprojects`/`allprojects`, cross-project mutation, and `afterEvaluate`. Compose's Material 3 artifacts have an independent version; do not assume every Compose artifact uses the CMP plugin version.

The shared Compose convention enables Android resources for every consumer. Generated `Res` accessors can compile while their assets are absent from an APK when resource support is disabled. Its TestKit fixture builds an AAR and checks a real drawable entry; changing resource conventions also requires inspecting final APK assets. See [Compose resource setup](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-setup.html#resources-in-the-androidlibrary-target).

Host UI testing is opt-in; plain shared libraries do not inherit Robolectric. The app's real navigation journey runs under `:shared:testAndroidHostTest`, included in `verify.sh`. Shared vectors use literal colors, not Android resource references: packaged XML can still fail at runtime when Compose parses unsupported values.

Tests use `commonTest`, Android-only `androidHostTest`, and opt-in `androidDeviceTest`. Existing JVM tests also use explicitly registered `src/test/java` directories where retained. Never invoke aggregate tasks that might execute device tests. `./scripts/verify.sh` checks conventions, host tests, Android lint, and debug packaging; `./scripts/verify-ios.sh` builds unsigned with the physical-device SDK on macOS. Device-test APKs are compiled separately and executed only by the user on physical devices.

Run `./gradlew -p build-logic :convention:check` when changing conventions. TestKit covers Android defaults/add-ons/overrides, shared target/dependency isolation, and configuration-cache reuse. Configuration cache remains opt-in. Use focused checks during iteration and the full verification scripts before handoff.

See [multiplatform ownership/setup](../docs/workflow/multiplatform.md) and [backend portability](../docs/workflow/open-source.md). Existing feature and contract/internal boundaries remain; only platform integrations belong in native source sets. The sole authorized destructive reset is the transition from old account databases to device-profile version 7.

References reviewed 8 November 2026: [Now in Android conventions](https://github.com/android/nowinandroid/blob/main/build-logic/README.md), [Jetchat's simpler direct build](https://github.com/android/compose-samples/blob/main/Jetchat/app/build.gradle.kts), [Gradle conventions](https://docs.gradle.org/current/userguide/implementing_gradle_plugins_convention.html), [built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin), [Android KMP libraries](https://developer.android.com/kotlin/multiplatform/plugin), and [incremental migration](https://kotlinlang.org/docs/multiplatform/migrate-from-android.html). Use sample patterns selectively; do not copy signing credentials, managed devices, or application services.

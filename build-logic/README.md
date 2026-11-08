# Build conventions

Modules select small, additive plugins from the included `build-logic` build. SDK versions and external plugin dependencies come from the root version catalog; Java/Kotlin Android bytecode targets Java 17. AGP supplies built-in Kotlin using the new DSL. Do not apply `org.jetbrains.kotlin.android`.

| Plugin | Provides | Current consumers |
| --- | --- | --- |
| `movers.android.application` | Android application, SDK/JVM defaults | `:app` |
| `movers.android.library` | Android library, SDK/JVM defaults | All Android libraries, including contracts |
| `movers.android.test` | Standalone Android test module, SDK/JVM defaults | `:tests` |
| `movers.android.compose` | Compose compiler, Compose BOM (including Android tests), previews and debug tooling | App, Auth, Profile, presentation, catalogue, details, watchlists, tests |
| `movers.android.hilt` | Android Hilt plugin/runtime, KSP and Hilt processor | App, Auth, Profile, catalogue, details, watchlists, database, movie internal, network |
| `movers.kotlin.serialization` | Serialization compiler plugin | App and serializable feature/Auth/Profile key contracts |
| `movers.android.unit-testing` | JUnit 4, MockK, coroutine-test | Modules previously declaring host-test dependencies |

Apply one Android base plugin. Add only the capabilities that module needs; Android add-ons also work when declared before the base. Unit testing applies to application/library modules, not standalone device-test modules. Serialization only applies its compiler plugin; keep runtime dependencies explicit.

For a new module, add its settings include, choose conventions, and declare its namespace and dependencies locally. Keep application identity/version, release shrinking/ProGuard rules, Room processors/schema paths, instrumentation dependencies, and mock Android return-value behavior local. Feature/project dependencies and UI libraries such as Material are not build defaults. Add a shared helper only for repeated policy; a one-off setting belongs in its module. Avoid root `subprojects`/`allprojects`, cross-project mutation, and `afterEvaluate` hooks.

Run `./gradlew -p build-logic :convention:check` from the repository root when changing conventions. Gradle TestKit fixtures check defaults, opt-in isolation, module overrides, add-on ordering, the Android test-module configuration, and configuration-cache reuse. They configure builds without launching devices. The full `./scripts/verify.sh` also checks these plugins before app checks. Global configuration-cache enablement remains opt-in.

## Android/iOS direction

The agreed future goal is shared Kotlin logic and Compose UI with thin Android/iOS hosts. These conventions configure the current Android app only; there are no KMP targets or iOS modules yet.

Future shared modules need separate conventions for `org.jetbrains.kotlin.multiplatform` plus `com.android.kotlin.multiplatform.library`, and Compose Multiplatform where needed. Do not apply these Android library/Hilt conventions to shared modules. The Android KMP plugin has different source sets, a single variant, and opt-in testing; it cannot inherit ordinary Android build-type assumptions. Decide the shared DI approach during the migration; Hilt stays in Android wiring.

Plan migration incrementally: portable contracts/models, data/platform adapters, then UI/navigation/resources and hosts. Audit current Android context/resources, Hilt annotations, Credential Manager, Retrofit/Gson integration, Java crypto/time/concurrency APIs, and navigation serialization before moving each owner. Libraries such as Room, DataStore, Coil, and Nav 3 have multiplatform offerings; their current usage still needs review. Keep repositories dependent on the [vendor-neutral database API](../docs/workflow/open-source.md) when Shared Lists is implemented.

## References

Reviewed 8 November 2026:

- [Now in Android build logic](https://github.com/android/nowinandroid/blob/main/build-logic/README.md): included build and composable, single-purpose conventions.
- [Jetchat app build](https://github.com/android/compose-samples/blob/main/Jetchat/app/build.gradle.kts): a smaller sample of direct Android build configuration; it is not the convention-plugin template.
- [Gradle convention plugins](https://docs.gradle.org/current/userguide/implementing_gradle_plugins_convention.html) and [Android built-in Kotlin migration](https://developer.android.com/build/migrate-to-built-in-kotlin).
- [Android KMP library plugin](https://developer.android.com/kotlin/multiplatform/plugin) and [incremental Android-to-KMP migration](https://kotlinlang.org/docs/multiplatform/migrate-from-android.html).

Use sample patterns selectively; do not copy signing credentials, flavors, managed devices, or application-specific services. Agents never start emulators/simulators or execute instrumented tests; runtime acceptance belongs on the user's physical devices.

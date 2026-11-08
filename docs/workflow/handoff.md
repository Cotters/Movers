# Current handoff

Updated: 8 November 2026. Refresh this document after meaningful progress; keep durable rules in the guides and active context here. Recheck claims against current code and Git state when resuming.

## Objective and status

The agreed bottom-navigation/Nav 3 implementation is complete and host-verified. **Physical-device acceptance remains pending.** Home, Watchlists, and Profile are implemented as retained tabs; Auth now lives inside Profile rather than a modal. Read [navigation ownership and acceptance](navigation.md) for the current design and exact device checks.

The convention-plugin refactor is implemented: Android modules select composable plugins from the included `build-logic` build, with AGP built-in Kotlin and the new DSL. See [build conventions and Android/iOS direction](../../build-logic/README.md). Implementation started from clean committed navigation baseline `454e313`; do not treat older references to uncommitted navigation changes below as current Git state.

Host verification, release shrinking, test-APK compilation, and real debug-build configuration-cache reuse passed. Android Studio sync, physical-device acceptance, and remote GitHub CI remain unverified. No application Kotlin source or database schema changed.

Shared Lists backend functionality and KMP module migration have not started. Supabase remains a candidate with no provisioned integration. The agreed future direction is shared Kotlin logic and Compose UI on Android/iOS with thin platform hosts; this task prepares only the Android build.

## Agreed decisions

- Codex-first guidance, with root `AGENTS.md` pointing to this tracked folder.
- `Human.md` is a brief public explanation plus an editable section for workflow issues/proposals. Preserve user feedback; proposals are not automatically policy.
- Agree on feature plans first; then complete authorized work and host checks without repeated permission requests.
- Keep private `Notes/Revision.md` ignored. Curate verified knowledge into tracked guides, rather than publishing personal study notes.
- Include local checks and CI. Repair setup-induced failures only; unrelated code/tooling issues require a separate agreed task.
- Never use emulators/simulators. Physical-device verification is user-owned and remains pending until the user supplies evidence.
- Shared means the user and a friend add to and review watchlists on separate devices. Supabase Free is the preferred backend candidate; no backend or integration has been created.
- Navigation uses **Nav 3**, serializable **NavKeys**, feature modularization, and deliberate ViewModel scoping. Tabs are **Home** (current feed), **Watchlists**, **Profile**. The bottom bar stays visible on main screens/details and inside Profile authentication. Guests see login/signup in Profile and Auth Required in Watchlists. Successful login shows Profile in place; logout, expiry, and account replacement reset retained navigation and return Home. List functionality remains deferred.
- Authenticated Profile and bookmarks retain their existing behavior. Revisit their relationship to watchlists later; the private todo records this reminder.
- Tab switching preserves history; reselection pops to the selected tab root without resetting root scroll. Back at another tab root selects Home's retained stack; Home root exits.
- Use additive convention plugins, keep module-specific configuration local, and preserve the current dependency versions. Android Hilt/Compose setup is opt-in. Future KMP conventions must be separate; migration and shared DI require a separately agreed plan.
- Treat this resume/portfolio project as public source. Repositories expose app operations and depend on a **separate vendor-neutral database API**; Supabase-specific adapters implement it behind Hilt bindings. See [portability guidance](open-source.md). No provider-specific SDK types should escape these boundaries.

## Verification evidence

Source baseline: `846baa7`, branch `SharedWatchlists`; working tree was clean before setup edits on 8 November 2026.

- Planning-stage `./gradlew testDebugUnitTest lintDebug :app:assembleDebug --console=plain` succeeded. XML reports contained **126 tests across 22 suites**, with zero failures, errors, or skips. Some tasks were `UP-TO-DATE`; this was not a clean rerun of every test.
- App lint XML contained **32 warnings**. Gradle also reported existing AGP/Kotlin/DSL deprecation warnings. These were not changed or suppressed.
- JDK 17 and Gradle 9.5.0 were confirmed. The task graph also resolved in a dry run, which by itself did not execute tests.
- No device/emulator/simulator was used. Device/UI behavior, instrumented tests, and remote CI execution are unverified.
- Setup verification on 8 November 2026, against `846baa7` plus the uncommitted workflow changes:
  - `./scripts/verify.sh` succeeded in the main workspace (8 seconds); unit-test tasks reused up-to-date results.
  - The same script succeeded in a fresh temporary source copy containing tracked source plus the workflow changes, without `local.properties` or local credentials. The exact CI step generated the placeholder constant; its overwrite guard was also verified. **All 509 actionable Gradle tasks executed** (40 seconds). Fresh XML reports contained **126 tests across 22 suites**, zero failures/errors/skips; app lint still had **32 warnings**. This was a macOS host run, not a Linux/GitHub execution.
  - Actionlint 1.7.12 accepted `.github/workflows/android.yml` (`-shellcheck=`; ShellCheck was not installed). `bash -n` accepted the verification script and each workflow shell block; `git diff --check` passed.
  - Ten isolated script checks passed: normal invocation, Gradle exit-code propagation, unsupported arguments, wrong/missing JDK, invalid/missing SDK configuration, local-properties fallback, credential preservation, and missing-credential handling. The normal case also verified invocation from outside the source directory and the exact Gradle arguments.
  - All **19 local Markdown links** resolved, `Human.md` contained **194 words**, and the new files were confirmed not ignored. Private `Notes/` remains ignored and unchanged.

## Open work and next action

1. **Navigation device acceptance:** user runs the checks in [navigation.md](navigation.md#physical-device-acceptance) and the compiled `MoversNavigationShellTest` on a physical device. Record actual observations before checking off the private Navigation Todo. No emulator/simulator use or automatic instrumented execution.
2. **Shared Lists:** separately agree account integration/local-data migration, membership/invites, allowed actions, offline/sync behavior, and two-device acceptance. Repositories must depend on a separate vendor-neutral database API, with Supabase-specific adapters behind it. No database API or backend integration was added by the navigation pass.
3. **Multiplatform:** agree an incremental shared Kotlin/Compose migration for Android/iOS. Review the platform constraints in build guidance before moving modules; no targets, iOS host, or shared DI implementation were added by the build refactor.
4. GitHub CI remains unverified remotely in this chat. No push, publishing, backend provisioning, repository-settings change, or license selection was performed.

### Documentation follow-up, 8 November 2026

Reviewed source at `11683fc` and current official Android/Supabase guidance. Added the private todo, the tracked public-source/portability guide, and current requirements above. This is documentation/planning only; prior host-test evidence does not verify the future Nav 3 migration.

`git diff --check` passed and all **23 local Markdown links** resolved. `Human.md` is **197 words**; private `Notes/Todo.md` is **61 words**, with exactly two unchecked work items, and remains ignored. No app code, dependencies, database, or CI behavior changed, so no build/device checks were repeated. Those planning questions were subsequently resolved: Auth forms live inside Profile with the bar visible; Back exits through Home, and tab reselection pops to root. The earlier modal-Auth proposal is superseded.

Historical study notes also identify auth submission/session-recovery, release logging/backup policy, and Android-runtime checks as follow-ups. Reinspect source before treating any item as an active defect. No historical test count is evidence for untested later changes.

## Nav 3 implementation evidence, 8 November 2026

Implementation started from clean source `aec6302`; results below cover the uncommitted navigation changes. New contract modules own feature keys; feature modules supply Hilt entry providers; the app owns tab state and session reconciliation. `IUserRepository`, `UserSession`, and `MoverUser` moved to Auth's contract with unchanged packages.

- `./scripts/verify.sh` passed: **130 host tests across 23 suites**, no failures/errors/skips, Android lint and debug APK successful. Some tasks reused up-to-date results. App lint still reports **32 warnings**; existing AGP/Kotlin/DSL warnings remain.
- `:app:testDebugUnitTest` passed after strengthening key round-trip coverage to use Nav 3's actual polymorphic `NavKeySerializer` across feature contracts.
- `./gradlew :app:assembleRelease :app:assembleDebugAndroidTest :auth:assembleDebugAndroidTest :features:catalogue:assembleDebugAndroidTest :features:details:assembleDebugAndroidTest --console=plain` passed. Release minification and test APK compilation succeeded; the final artifact refresh also passed after source cleanup (82 seconds, with up-to-date task reuse). **Instrumented tests were not run**.
- The final debug runtime dependency report resolved successfully and contains no Nav 2 or `hilt-navigation` artifacts. Dependencies use Nav 3 1.2.1, Lifecycle 2.11.0, and AndroidX Hilt ViewModel Compose 1.4.0.
- Reviewed the installed Lifecycle 2.11.0 decorator/provider sources for inactive-stack retention and generation disposal. Host policy tests cannot prove UI/store lifetimes; compiled device tests cover that remaining runtime verification.
- Credential Manager invocation now belongs to resumed Auth UI, once per entry. Auth ViewModels receive plain results, do not hold Activity contexts, and clear passwords on successful authentication. NavKeys contain no credentials.
- Final `./scripts/verify.sh` rerun passed after source cleanup: 130 tests across 23 suites, zero failures/errors/skips; lint and debug APK passed (27 seconds, with up-to-date task reuse). All 28 local Markdown links resolved, new XML resources parsed, and `git diff --check` passed. Human.md remains 197 words; Notes/Todo.md remains ignored. Device/UI checks, process restoration on a real device, and remote CI execution remain pending.

## Convention-plugin implementation evidence, 8 November 2026

Source: implementation began at `454e313` and was committed as `c12699d` while final verification was running; the final verification notes were added afterward. The included build owns seven composable plugins; module namespaces, project edges, release rules, Room schema configuration, and mock Android behavior stay local. Removed legacy Kotlin/new-DSL opt-outs and redundant library optimization declarations. Removed the unused Compose compiler plugin from movie internal after checking its source; retained its runtime dependencies/resources. No dependency versions were upgraded.

- `./gradlew help` and `./gradlew build --dry-run` passed. The dry run only validates task-graph configuration; it does not run device tests. The installed Android CLI successfully searched/fetched official built-in Kotlin documentation, alongside current official AGP/Gradle/KMP references and Google samples linked in build guidance.
- `./gradlew -p build-logic :convention:check` passed, including strict plugin validation and **4 executed TestKit tests**. Fixtures cover plain-library isolation/defaults/overrides, app/library add-on combinations and ordering, standalone Android tests, and configuration-cache reuse. No devices were started.
- Initial migrated `./scripts/verify.sh` passed with **130 app host tests across 23 suites**, zero failures/errors/skips, lint and debug APK successful; app test tasks executed. App invocation: 814 actionable tasks, 810 executed and 4 up-to-date. App lint remains at **32 warnings**.
- Final `./scripts/verify.sh` passed after module-script cleanup: build-logic checks reused results; app verification took 28 seconds, with 96 executed and 718 up-to-date tasks. Test XML still reports 130 app tests and 4 build-logic tests with no failures/errors/skips; reused results are not a fresh test execution.
- `:app:assembleRelease` plus debug Android test APKs for app, Auth, catalogue, details, database, and standalone `:tests:assembleDebug` passed. This covers shrinking, Hilt/KSP/Room generation, and device-test compilation, **not execution**. The combined invocation reported 55m 41s, with 803 tasks executed and 406 up-to-date; this was not a controlled build-time comparison. Investigate reproducible release slowness separately before drawing performance conclusions.
- Both debug/release merged manifests retain minimum SDK **28** and target SDK **36**. Compile SDK remains **37**. Plugin dependency inspection confirms AGP **9.3.3**, Kotlin **2.4.10**, KSP **2.3.10**, Hilt **2.60.1**, Gradle **9.5.0**, and JDK **17**. Debug runtime dependencies still contain no Nav 2 or `hilt-navigation` artifacts.
- Two real `:app:assembleDebug --configuration-cache --configuration-cache-problems=fail` invocations passed; the second reused the cache (14 seconds). Cache remains opt-in globally. TestKit independently verifies reuse in a fixture.
- Existing deprecated `createComposeRule` warnings appeared during test-APK compilation; native-library stripping notes also remain. No blanket suppression or unrelated Compose-test migration was introduced.
- Bash syntax, workflow YAML and both workflow shell blocks passed. Four isolated verification-script scenarios passed: successful invocation/order from outside the repo, immediate stop with the convention check's failure code, propagation of the app check's failure code, and argument rejection. Actionlint was unavailable; GitHub execution remains pending.
- Workflow guidance, the build-logic README, root agent routing, and private queue were updated. Human.md remains concise at **202 words**, and its feedback section is preserved. Private Notes remain ignored. Follow [sync/device acceptance](testing.md#convention-plugin-migration-acceptance); navigation's existing physical-device checklist still applies.

Future KMP work must separately address platform-dependent DI, credentials, resources/context, Java APIs, networking adapters, and navigation serialization. No KMP targets, iOS host, shared DI choice, or backend integration were added. The database portability policy remains unchanged.

# Current handoff

Updated: 8 November 2026. Refresh this document after meaningful progress; keep durable rules in the guides and active context here. Recheck claims against current code and Git state when resuming.

## Objective and status

The agreed bottom-navigation/Nav 3 implementation is complete and host-verified. **Physical-device acceptance remains pending.** Home, Watchlists, and Profile are implemented as retained tabs; Auth now lives inside Profile rather than a modal. Read [navigation ownership and acceptance](navigation.md) for the current design and exact device checks.

Shared Lists backend functionality has not started. The private `Notes/Todo.md` queue records Navigation device acceptance, then Shared Lists, then adding/adopting convention plugins. Supabase remains a candidate with no provisioned integration.

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
- Tab switching preserves history; reselection pops to the selected tab root without resetting root scroll. Back at another tab root selects Home's retained stack; Home root exits. Keep current Gradle scripts; convention plugins are a separate Todo item.
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
3. **Convention plugins:** introduce/adopt in a separate agreed task; current module scripts/version catalog remain authoritative.
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

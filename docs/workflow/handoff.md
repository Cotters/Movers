# Current handoff

Updated: 8 November 2026. Refresh this document after meaningful progress; keep durable rules in the guides and active context here. Recheck claims against current code and Git state when resuming.

## Objective and status

Workflow setup is implemented. Current work is to record the ordered queue, research public-source/backend portability, and agree the first Navigation pass. Navigation code and Shared Lists implementation have not started; no backend has been provisioned. The first GitHub-hosted CI run remains pending in the evidence available to this chat.

The user has requested Navigation first, then Shared Lists. Agree the Navigation implementation plan before changing app code. Private `Notes/Todo.md` is the concise ordered queue; its requirements are also recorded here for fresh clones.

## Agreed decisions

- Codex-first guidance, with root `AGENTS.md` pointing to this tracked folder.
- `Human.md` is a brief public explanation plus an editable section for workflow issues/proposals. Preserve user feedback; proposals are not automatically policy.
- Agree on feature plans first; then complete authorized work and host checks without repeated permission requests.
- Keep private `Notes/Revision.md` ignored. Curate verified knowledge into tracked guides, rather than publishing personal study notes.
- Include local checks and CI. Repair setup-induced failures only; unrelated code/tooling issues require a separate agreed task.
- Never use emulators/simulators. Physical-device verification is user-owned and remains pending until the user supplies evidence.
- Shared means the user and a friend add to and review watchlists on separate devices. Supabase Free is the preferred backend candidate; no backend or integration has been created.
- Navigation uses **Nav 3**, serializable **NavKeys**, feature modularization, and deliberate ViewModel scoping. Tabs are **Home** (current feed), **Watchlists**, **Profile**. The bottom bar stays visible on main screens/details; Auth is modal and hides it. First pass handles navigation, with list functionality deferred.
- Profile and bookmarks remain unchanged. Revisit their relationship to watchlists later; the private todo records this reminder.
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

1. **Navigation:** finalize modal Auth presentation, guest access/return behavior, back-stack policy, feature API/implementation boundaries, and entry-scoped ViewModels; present the concrete plan for agreement. Preserve feed state, poster transitions, session isolation, Profile, and bookmarks. Google's Navigation 3 and edge-to-edge skills have been read for this planning work; no migration code has been written.
2. **Shared Lists:** later agree account integration/local-data migration, membership/invitations, allowed actions, offline/sync behavior, and acceptance checks on two physical devices. Include backend API/adapters, reproducible deployment, and credentials-free evaluation in that plan. A demo adapter is proposed, not yet selected or built.
3. Public backend schemas/migrations/adapters and synthetic fixtures make setup reproducible; live data and privileged credentials remain private. Another Supabase instance is a configuration/setup change; another provider requires a compatible adapter/auth integration. No license has been selected.
4. GitHub CI remains unverified remotely. No push, publishing, backend provisioning, or repository-settings change was performed.

### Documentation follow-up, 8 November 2026

Reviewed source at `11683fc` and current official Android/Supabase guidance. Added the private todo, the tracked public-source/portability guide, and current requirements above. This is documentation/planning only; prior host-test evidence does not verify the future Nav 3 migration.

`git diff --check` passed and all **23 local Markdown links** resolved. `Human.md` is **197 words**; private `Notes/Todo.md` is **61 words**, with exactly two unchecked work items, and remains ignored. No app code, dependencies, database, or CI behavior changed, so no build/device checks were repeated. Pending user choices: full-screen Auth modal versus sheet; guest Profile opens Auth versus a guest page; root Back returns Home versus visited-tab history. Nav 3 modularization and ViewModel-scoping design will be included in the implementation plan after these choices.

Historical study notes also identify auth submission/session-recovery, release logging/backup policy, and Android-runtime checks as follow-ups. Reinspect source before treating any item as an active defect. No historical test count is evidence for untested later changes.

# Current handoff

Updated: 8 November 2026. Refresh this document after meaningful progress; keep durable rules in the guides and active context here. Recheck claims against current code and Git state when resuming.

## Objective and status

The agreed workflow setup is implemented: tracked human/agent guidance, an executable local host-verification script, README links, and a GitHub Actions workflow. Local validation is complete; the first GitHub-hosted run remains pending.

Shared Watchlist feature work has **not started**. After workflow setup, wait for the user's feature request, clarify requirements, and agree on a feature plan before implementation or backend provisioning.

## Agreed decisions

- Codex-first guidance, with root `AGENTS.md` pointing to this tracked folder.
- `Human.md` is a brief public explanation plus an editable section for workflow issues/proposals. Preserve user feedback; proposals are not automatically policy.
- Agree on feature plans first; then complete authorized work and host checks without repeated permission requests.
- Keep private `Notes/Revision.md` ignored. Curate verified knowledge into tracked guides, rather than publishing personal study notes.
- Include local checks and CI. Repair setup-induced failures only; unrelated code/tooling issues require a separate agreed task.
- Never use emulators/simulators. Physical-device verification is user-owned and remains pending until the user supplies evidence.
- Shared means the user and a friend add to and review watchlists on separate devices. Supabase Free is the preferred backend candidate; no backend or integration has been created.

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

1. The user can review `Human.md` and add workflow issues or proposals there. Workflow setup is ready for review; do not start feature work merely because setup is complete.
2. GitHub CI remains pending until the workflow is pushed and executed. No push, publishing, backend provisioning, or repository-settings change was performed.
3. On a later Shared Watchlist request, clarify account integration/local-data migration, membership/invitations, allowed actions, offline and sync behavior, and acceptance checks on two physical devices. Consult current Supabase documentation before selecting SDK/schema details.

Historical study notes also identify auth submission/session-recovery, release logging/backup policy, and Android-runtime checks as follow-ups. Reinspect source before treating any item as an active defect. No historical test count is evidence for untested later changes.

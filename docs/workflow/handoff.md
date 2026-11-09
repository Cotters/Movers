# Current handoff

Updated 9 November 2026. This is current project memory, not a transcript. Verify HEAD, working tree, and relevant source before acting. Past evidence is in [implementation history](history/2026-11-09-implementation.md); do not load it routinely or treat its permissions as current.

## State and decisions

- Current HEAD: `14cfd31` (`Improve list adding flow`), committed by the user during the retrospective. It includes the immediate-membership follow-up; this retrospective remains a documentation/tooling working-tree change. Preserve user edits/staging; no agent commit or push. Recheck Git before acting.
- Android and iOS share KMP logic and Compose Multiplatform UI. Thin hosts compose feature-owned Koin definitions, typed Nav 3 entries, entry-scoped ViewModels, Ktor, paging, and Room. Use [module ownership](multiplatform.md) and actual dependency declarations; no incidental toolchain upgrades.
- Home is the infinite catalogue plus global search; Watchlists supports local list creation/rename/delete and movie removal; Profile shows Josh/bookmarks. Tabs retain histories. The bottom bar remains visible on main destinations; quick-add overlays its source.
- **Current membership UX:** check a list to save immediately; uncheck to remove. The picker stays open until Close and has no confirmation button. Existing memberships are editable. Inline creation automatically adds the movie; a failed add keeps the created list available. Contextual list search toggles Add/red Remove without leaving results. Persisted observation owns membership; each pending row is temporarily disabled. A target list positions the picker without adding on open.
- Search debounces typing by 600 ms, supports immediate keyboard submission, and skips blank/unchanged queries. Saved query/navigation/scroll survive returning from quick-add. Lists persist offline; fresh TMDB search requires network. Duplicate names remain allowed with a non-blocking case-insensitive warning. Bookmarks remain independent.
- No accounts: one local installation ID and temporary name **Josh**. Onboarding/name entry is deferred. The ID is not backend authorization.
- Room **8** preserves version-7 profile/bookmarks/cache through `WatchlistMigration7To8`. Destructive fallback remains restricted to retired versions **1–6**. Never extend that exception incidentally.
- `IWatchlistsRepository` uses the neutral `WatchlistDatabase`; Koin binds the internal Room adapter. Sharing, invites, backend authorization/sync and Supabase provisioning require a separate agreed plan. Follow [public-source boundaries](open-source.md).

## Active workflow

Run existing tests for app changes; update intended expectations, but add **no new test cases, especially UI tests**, unless requested. Broader coverage is a post-feature Todo. Never run/interact with emulators/simulators or execute connected tests. People supply physical-device acceptance. Earlier device exceptions are historical.

Use [development guidance](development.md) for bounded debugging, memory, skills, and review; [testing guidance](testing.md) for selecting checks. The [retrospective](retrospective-2026-11-09.md) explains the changes. Documentation-only work uses `./scripts/verify-docs.sh`, without rebuilding apps.

## Latest app verification — before this documentation retrospective

Evidence covers `e407ed6` plus the immediate-membership working tree on 9 November, not arbitrary later edits:

| Check | Observed result | Limit |
| --- | --- | --- |
| `./scripts/verify.sh` | 92 tests / 24 suites, zero failures/errors/skips; five convention tests reused passing outputs. Final app phase: 32 s, 33 executed / 506 up-to-date. Lint/debug APK pass; 30 lint warnings, zero errors. | Some suites cached; Android host evidence. |
| `./scripts/verify-ios.sh` | Exit 0; unsigned physical-iOS app built after changed production sources. | Compile/link only, no runtime/signing. |
| Retained Android test APKs | Both compile: 2 s, 22 executed / 314 up-to-date. | No instrumented execution. |
| Existing Maestro watchlist flow | Syntax check passes. | Not executed natively for this follow-up. |

Known host trap: Paging's process-wide UI dispatcher can retain queued work between Robolectric cases. `WatchlistsUiTest` drains the main queue in an **outer TestWatcher after Compose-rule disposal**; `@After` runs too early. Keep this fix in host testing. Timeout diagnostics include the expected text and semantics tree.

## Pending / next

- User verifies immediate add/remove, inline creation, keyboard layout, source retention, relaunch persistence, offline/retry and Back on physical Android/iPhone; record device/OS and sanitized observations. Host results do not establish native acceptance.
- Before claiming older-iOS support, resolve the recorded bundled ICU Simulator object warning (built for 18.5 versus configured minimum 15.0). Historical native smoke evidence covers iOS 26.2 only; no minimum-version change is authorized by this note.
- Remote CI, iOS CI, signing/distribution, iOS edge gestures and broader accessibility remain unverified.
- For the next feature, read the first unchecked private `Notes/Todo.md` item when available and agree scope. In a clone without Notes, ask which feature to plan; do not infer collaboration authorization from local lists.

## Retrospective verification — 9 November 2026

`./scripts/verify-docs.sh` passes from the repo and an unrelated working directory: 11 documents and 58 local link targets. Shell syntax, CI YAML/embedded shell blocks and `git diff --check` pass. The checker does not validate external URLs/anchors; research sources were opened separately. No app code, dependencies or tests changed during this retrospective, so app builds/suites were not rerun. The existing app evidence above remains separate; the new CI step has not executed remotely.

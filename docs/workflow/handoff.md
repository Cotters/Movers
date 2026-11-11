# Current handoff

Updated 11 November 2026. Baseline: `7ad8b50` (first five small Todo items). User authorized the next few clear backlog actions, planned and committed separately: random list picks, skippable feature tour and movie trailers. UI refresh explicitly requires discussion and is skipped; watched status still needs product design. No emulator/simulator/connected tests, automatic builds, new test cases, backend deployments or releases. [Earlier work and release evidence](history/2026-11-11-before-next-features.md) does not verify these new changes.

## Next feature pass

- **List picks implemented:** personal/shared list menu → Pick a film → existing detail entry. Picks from the full list, ignoring the search filter; pending/committing/removing films are excluded. A small shared selection policy gives valid TMDB ratings linear weights 1–10, with weight 5 for unavailable/invalid/unrated values. All films remain eligible; no remote lookup or list writes. See [selection design](../features/list-picks.md). Diff/source/docs review only; physical acceptance pending.
- **Next:** brief, one-time skippable feature tour after name confirmation; then TMDB trailer discovery with an embedded player and external fallback. Preserve the existing name flow, provider-neutral movie contract, cached details and Nav3 entry ownership.

## Current boundaries and acceptance

- Shared CMP/Nav3/Koin/Room architecture and existing convention plugins remain. Room 10 preserves migrations from 7 onward; no new schema required for picks. Repository/provider separation and private configuration stay intact.
- Prior five changes: encryption exemption persists in plist/XcodeGen; list editing exposes only layout/Done; import Done/background taps dismiss the keyboard; Profile configures the stable eight-colour avatar palette; one Import / export item opens inline export plus the existing import session. Existing persistence/serialization assertions were updated, with no new tests/builds in that pass.
- Existing swipe Undo hides films immediately and defers writes until snackbar dismissal/expiry; app termination before commit cancels pending intents. Root snackbar survives navigation. Preserve attribution/activity on Undo and restore failed removals. See [swipe design](../features/swipe-actions.md).
- Physical checks remain user-owned: Android/iPhone navigation/keyboard and short-list clipping, prior UI changes, native import/export and two-device shared attribution/colour updates. Backend import deployment was user-confirmed. Prior 0.2.0 (3) upload/build evidence is archived; later commits are not in that uploaded build.
- Feature-phase policy: maintain existing assertions; defer new coverage to the private Todo. Use source/diff/docs checks for ordinary edits. Explain a concrete substantial/native build need before any compilation-backed verification. Never claim source review proves native runtime behavior.

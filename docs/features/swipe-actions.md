# Film-row swipe actions

Agreed 11 November 2026. Personal/shared watchlist list rows reveal Add to list at the start and Remove at the end; Home list rows reveal Add to list only. Directions mirror in RTL. Swiping reveals an explicit button, never a full-swipe destructive action. Existing +/Edit controls and grid cards stay unchanged. Add uses the retained multi-list picker.

## Undo and writes

- Swipe removal hides a film immediately in the list projection. The app-owned coordinator waits for the Undo snackbar to expire or be dismissed before calling the existing repository removal. Undo cancels the pending intent: no remove/re-add cycle, replacement contributor/timestamp or false activity.
- Rapid removals join one batch and refresh its snackbar; Undo cancels the entire displayed batch. Material's Long snackbar duration respects accessibility timeout settings. Queued snackbars do not shorten the visible Undo window.
- Undo belongs to the app shell, so leaving the list or switching tabs does not lose it. Pending intents are session-only: terminating the app before the decision cancels them, as explicitly agreed. Once a write begins, existing repository cancellation/idempotency/reconciliation rules apply.
- Projection always uses current repository content. Undo does not resurrect a film a collaborator independently removed. Failed writes release their hidden rows and show a concise error; another swipe can retry.
- Edit-mode removal remains its existing immediate action. No new storage contract, database schema, migration, backend operation or dependency.

## Ownership and verification

Presentation owns the small Foundation anchored-drag wrapper, explicit action panels and equivalent accessibility custom actions. Watchlists owns the Koin-managed removal coordinator, its lifetime, pending projection and snackbar decisions. App composition installs the message effect; the standalone navigation shell remains injectable and can run without feature DI. Vertical scrolling, normal film taps and poster transitions retain their existing ownership.

Reviewed [RevealSwipe](https://github.com/ch4rl3x/RevealSwipe) (CMP, Apache-2.0) as inspiration; no library code copied or dependency added. Actual Foundation 1.12.2 anchors/fling APIs were checked in cached sources against [Android dragging guidance](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-composables) and [snackbar action guidance](https://developer.android.com/develop/ui/compose/components/snackbar).

Existing host checks passed: Watchlists 2 tests and shared/navigation 19; shared checks reran after shell-effect isolation. No new test cases, device/simulator interaction, iOS build or deployment. These checks cover existing flows and compilation, not new swipe/Undo journeys. Physical acceptance remains for Android/iPhone: both swipe directions, vertical/edge-back gestures, explicit action taps, multi-add source retention, single/batched Undo across navigation, timeout/dismiss commits, offline failure and concurrent collaborator changes. Test expansion stays in the post-feature queue.

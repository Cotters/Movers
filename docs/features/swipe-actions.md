# Film-row swipe actions

Updated 12 November 2026. Personal/shared watchlist film rows reveal Add to list at the start and Remove at the end; Home film rows reveal Add on the end, matching the later organization follow-up. Watchlist overview rows reveal Pin/Unpin at the end. Directions mirror in RTL. Grid cards and Edit controls stay unchanged. Add uses the retained multi-list picker.

## Short and full swipes

- Short swipes reveal the existing 88 dp action button. A fast short flick can reveal it but never activates it.
- Add and Pin/Unpin explicitly opt into full-swipe activation on Android/iOS. Drag past 65% of the row width (at least two action widths), then release. The exposed action surface uses accent/on-accent colours while armed; pulling back below the threshold disarms it. Rows too narrow to separate the two thresholds keep reveal-and-tap.
- **Remove is always reveal-and-tap only**, as confirmed by the owner. Destructive actions cannot full-swipe even if accidentally opted in. Disabled actions, including Pin at the cap, cannot arm or activate.
- On release, the row closes before performing the action once. Cancelling a gesture closes without acting; another drag interrupts settling. Buttons and accessibility custom actions remain available. No membership write or activity is caused by revealing, arming or cancelling.

## Undo and writes

- Swipe removal hides a film immediately in the list projection. The app-owned coordinator waits for the Undo snackbar to expire or be dismissed before calling the existing repository removal. Undo cancels the pending intent: no remove/re-add cycle, replacement contributor/timestamp or false activity.
- Rapid removals join one batch and refresh its snackbar; Undo cancels the entire displayed batch. Material's Long snackbar duration respects accessibility timeout settings. Queued snackbars do not shorten the visible Undo window.
- Undo belongs to the app shell, so leaving the list or switching tabs does not lose it. Pending intents are session-only: terminating the app before the decision cancels them, as explicitly agreed. Once a write begins, existing repository cancellation/idempotency/reconciliation rules apply.
- Projection always uses current repository content. Undo does not resurrect a film a collaborator independently removed. Failed writes release their hidden rows and show a concise error; another swipe can retry.
- Edit-mode removal remains its existing immediate action. No new storage contract, database schema, migration, backend operation or dependency.

## Ownership and verification

The design system owns the shared gesture/panels/accessibility actions and a focused UI-local `SwipeActionsState` for anchors, velocity and settling. Foundation's horizontal drag detector distinguishes release from cancellation; anchored state still handles bounds and settling animations. Gesture/velocity tracking stays on the stationary row container while only the foreground moves. Add/Pin opt-in belongs to their presentation/feature adapters; repositories and removal policy remain outside the control. Watchlists owns the Koin-managed removal coordinator, its lifetime, pending projection and snackbar decisions. App composition installs the message effect; the standalone navigation shell remains injectable and can run without feature DI. Vertical scrolling, normal film taps and poster transitions retain their existing ownership.

Reviewed [RevealSwipe](https://github.com/ch4rl3x/RevealSwipe) (CMP, Apache-2.0) as inspiration; no library code copied or dependency added. Actual Foundation 1.12.2 anchors/fling APIs were checked in cached sources against [Android dragging guidance](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-composables) and [snackbar action guidance](https://developer.android.com/develop/ui/compose/components/snackbar).

Existing host checks passed: Watchlists 2 tests and shared/navigation 19; shared checks reran after shell-effect isolation. No new test cases, device/simulator interaction, iOS build or deployment. These checks cover existing flows and compilation, not new swipe/Undo journeys. Physical acceptance remains for Android/iPhone: both swipe directions, vertical/edge-back gestures, explicit action taps, multi-add source retention, single/batched Undo across navigation, timeout/dismiss commits, offline failure and concurrent collaborator changes. Test expansion stays in the post-feature queue.

Full-swipe follow-up: installed Foundation/UI 1.12.2 sources and current [dragging documentation](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-composables) reviewed. Code-review script, full-function/caller review and diff/docs checks only; no builds, app tests or new cases. Physical acceptance remains: short reveal/tap, deliberate long Add/Pin, pullback/cancel, repeated gestures, disabled pins, RTL and scrolling/back gestures. Long Remove must only reveal its button; tapping it must retain existing Undo. Earlier host evidence above predates this change.

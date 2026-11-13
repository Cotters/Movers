# Current handoff

Updated 13 November 2026. Task base `c040d7e`. Apple shared-watchlist notifications are implemented; **live provisioning and physical acceptance remain pending**. Backend and app integration are committed as `2c02615` and `37fde7b`; final review includes registration-ownership protection and composition-root notification effects. [Feature design](../features/watchlist-notifications.md), [deployment](../../supabase/NOTIFICATIONS.md), [previous host/cleanup evidence](history/2026-11-13-before-notifications.md).

## Current behavior and boundaries

- Other active members receive Apple alerts for committed additions/imports. Group by actor/list, five quiet minutes with no maximum delay; minute dispatch normally means five to six minutes. Publication/duplicates/removals are silent. Removed additions are excluded before preparation.
- Durable backend work commits with activity and survives app closure. Dispatch/registration RPCs are separately authorized; enqueue and dispatch default off. Server credentials stay out of clients. No Firebase, new app dependency, Room migration, identity replacement or live deployment.
- Swift handles permission/APNs registration, foreground banners/default sound and tap delivery. Permission is contextual to shared-list use after onboarding; Profile controls sit below avatar colours. Signed XML/DER entitlements select sandbox/production without assuming Release means production.
- Taps retain early targets through startup, select Watchlists and create one fresh detail entry with temporary newest-first/search reset. Saved sorting and other tabs remain intact. Explicit sort changes persist normally. The app composition root supplies notification effects to the otherwise isolated navigation shell.
- Preserve existing shared mutation receipts, cancellation, installation identity, Room 7→8→9→10 migrations, Undo/swipe behavior, poster/footer decisions, saved queries/sorts/pins and invitation handling. [Organization](../features/watchlist-organization.md), [swipes](../features/swipe-actions.md), [shared lists](../features/shared-watchlists.md).

## Verification and limits

- Studio CLI formatting, strict source-only Detekt, strict Swift formatting, Deno typecheck/lint/format and documentation checks pass. Full-function/caller review caught and corrected an isolated-shell dependency. Live IDE diagnostics are unavailable; the running user IDE was not stopped. Final focused shared host checks, existing test-APK compilation (no execution) and Android packaging pass.
- Full existing host suites, Android lint and debug packaging pass. No new automated cases; notification coverage expansion remains post-feature work.
- Full unsigned iOS Debug and Release archive pass with iOS 16 minimum, including the Swift/Kotlin bridge. The final Release archive includes the composition-root correction and emits no warnings/errors. App version/build remain 0.2.0 (3); no release bump or upload. Signing, APNs acceptance and runtime are unverified.
- Disposable PostgreSQL checks pass for all migrations/deployment metadata, replay/no-op/import counts, removal cancellation, recipient membership, registration ownership, stale leases, concurrent preparation/claiming and locked-list skipping. The local cluster is stopped. Synthetic Auth roles do not prove hosted Auth/Realtime/Vault.
- Generated-key ES256 verification, grapheme-safe notification rendering and local signed XML/DER extraction probes pass. Credential scans of task and staged files found no actual ignored-config values or private key blocks; values were never printed. Secrets-file instructions use an already ignored path.

## Next action

Implementation and host verification are complete; no signing, version bump, upload or backend activation is implied. User configures Apple capabilities/profiles and server secrets, then validates sandbox and TestFlight delivery before enabling production. See deployment instructions for the kill switch and overdue-work diagnostics.

User tests on physical devices: permission allow/deny and Settings changes; existing-member prompting; foreground/background alerts; sender closure after server confirmation; grouping/imports/quick removals; cold/warm taps; temporary sort/search behavior; departed membership and offline cache. Include iOS 16 acceptance where available. Agents never use simulators/emulators or run connected tests. Keep credentials, private Notes and raw build logs untracked.

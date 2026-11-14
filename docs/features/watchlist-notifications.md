# Shared-watchlist notifications

Agreed 13 November 2026. Apple delivery first, directly through APNs. Firebase and Android receiving are deferred; additions from either platform trigger iOS alerts.

## Behavior

Notify other active members about committed additions and successful imports into existing shared lists. Personal lists, initial publication, duplicates, removals and metadata edits are silent. Group by contributor/list and become eligible five minutes after the last successful addition, without a maximum wait. The minute scheduler normally dispatches five to six minutes after the last addition, before APNs delivery time. Cancel removed additions before preparation; an empty group is silent. After preparation, later additions start another group.

Alerts use APNs priority 5, allowing Apple to defer delivery for device power considerations. Five to six minutes is the normal dispatch window, not a presentation deadline. The owner reported a successful physical-iPhone alert approximately eight minutes after adding a film on 14 November 2026; the additional delay has not been attributed to a specific stage.

Title is **Movers**. One film: `Rue added Inception to Weekend`. Multiple films, including imports: `Rue added 5 films to Weekend`. Watchlist names use ten grapheme clusters plus an ellipsis when longer. Actor/film text is bounded safely for Apple's payload limit. Foreground alerts use the same system banner/default sound, including on the open list. No badges, rich media, actions, silent background pushes or per-list mute in v1.

Request alert/sound permission on first shared-list use, including the next shared-list visit for existing members, after startup/modal flows finish. Denial never loops. Profile's Notifications controls sit below avatar colours and expose authorization, registration/retry and system settings. Notification taps open Watchlists → list, newest first with search cleared for that visit; saved preferences remain intact. No invitation is accepted and no membership is granted by a notification.

## Ownership and reliability

Film activity and durable notification items commit together on the server. Actor attribution and eligible recipients come from authenticated server state. Request receipts/no-op writes prevent duplicate work. Removed items are cancelled before preparation, and recipient membership is checked again on delivery; joining later does not grant historical alerts. App closure does not cancel committed work.

Postgres freezes groups and leases per-device deliveries. A scheduled, authenticated Edge Function signs/reuses environment-specific provider JWTs and sends direct HTTP/2 APNs alert requests. It never sleeps through the grouping window. Delivery records survive function termination; stale leases cannot finish newer attempts. APNs acceptance is recorded separately from presentation. Transient retries use backoff/jitter until 24 hours after the group's due time. Invalid tokens are disabled with registration-time protection. Stable collapse IDs reduce duplicate alerts after ambiguous network failures; exactly-once presentation is not guaranteed.

APNs keys/provider JWTs, tokens and bodies never enter app logs, navigation or public diagnostics. New private tables use RLS and no direct client grants. Registration RPCs derive ownership from authentication; worker RPCs accept only the server role. The worker requires its own dispatch secret before accessing work. Both database enqueue and worker dispatch default off. Existing events are never backfilled. See [deployment instructions](../../supabase/NOTIFICATIONS.md).

## Verification

Use the project's formatting/static review, existing host checks and unsigned physical-device SDK builds when warranted. No new automated cases during the feature phase. Disposable PostgreSQL transactions cover permissions, attribution, duplicate/replayed mutations, imports, batching, removals, recipients and lease recovery. Apple signing, hosted transport, APNs acceptance and presentation require separate deployment/physical-device acceptance.

The owner validates permission allow/deny and Settings changes, existing members, foreground/background alerts, sender closure after server commit, two additions inside/outside the grouping window, imports, quick removals, cold/warm taps, temporary preferences, departed memberships and offline cache behavior on physical devices. Agents never launch simulators/emulators or run connected tests. Expanded automated coverage remains post-feature work.

Sources: [Apple APNs architecture](https://developer.apple.com/documentation/usernotifications/setting-up-a-remote-notification-server), [registration](https://developer.apple.com/documentation/usernotifications/registering-your-app-with-apns), [permission](https://developer.apple.com/documentation/usernotifications/asking-permission-to-use-notifications), [APNs requests](https://developer.apple.com/documentation/usernotifications/sending-notification-requests-to-apns), [provider keys](https://developer.apple.com/documentation/usernotifications/establishing-a-token-based-connection-to-apns), [Supabase scheduling](https://supabase.com/docs/guides/functions/schedule-functions). Living documentation reviewed during planning. Apple's Notes documentation publishes no batching interval; this timing is a Movers product decision.

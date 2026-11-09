# Shared Watchlists

Agreed 9 November 2026. Shared Android/iOS UI and policy; Supabase is a replaceable adapter. This document records product decisions and the implementation contract. Current verification and deployment status belong in the [handoff](../workflow/handoff.md).

## Behavior

- Personal lists stay local/offline until the user chooses Share list. That action publishes immediately and opens the final sharing screen, with no introduction or confirmation step. Publishing preserves the list ID and imports existing films with server-time/publisher attribution; it does not invent earlier history.
- Active members have equal access: rename, add/remove films, invite, inspect members/activity, and leave. Creator is attribution only. No kicking, ownership transfer, or global shared-list deletion. Last departure retires the list and revokes invitations.
- Shared contents are cached in Room for offline reading. Shared writes require a connection and server confirmation. Instant check/uncheck and plus/red-trash actions keep their source screen open. Film actions have accessible labels and show saving progress. List-detail trash controls are hidden until Edit list is enabled in its dropdown; Done or Done editing exits that mode. Delete, Leave and Revoke actions use the theme's error color and retain confirmation.
- A confirmed name is required at startup. Existing Josh profiles retain their installation ID/data and confirm or replace that name once. First-launch name entry is now in scope; the feature tour remains deferred.
- Device identity uses anonymous backend authentication without login UI. No recovery/device linking in v1. Names are editable, not unique, and never authorize access. Profile edits require an online save once a backend identity exists.
- Avatars use an initial and a randomly assigned, persisted palette color. Names remain accessible. Color selection is a future improvement; no profile pictures.
- List metadata includes creator, members/count, and last-updated time. Current entries show contributor/time. Activity is a separate paginated, day-grouped screen of film additions/removals only, retaining actor snapshots and removed-film metadata.

## Invitations

Use native sharing and `movers://join/<token>` with a paste fallback on Watchlists. Handle cold/warm links and defer through name setup. Incoming tokens remain outside persisted navigation state. Opening an invitation joins immediately and opens the list. Pasting an invitation uses one Join action, without a preview step. This supersedes the original explicit-preview flow at the user’s request. Failed joins stay on a concise retry/link-entry screen; the backend still validates the invitation and membership transaction.

The sharing screen shows the list name, link access/expiry, Share link and Revoke links. Keep copy short; device-identity/recovery explanations live in project guidance rather than repeated sharing screens. Revoking all links still requires its existing brief confirmation.

Links are multi-use, random 256-bit values, stored as hashes on the server and valid for seven days. Any member can revoke all outstanding invitations. Existing members retain access. An issuer leaving does not invalidate a link while the list remains active. An already joined person opens the same list. No domain, website or install fallback is required in v1.

## Boundaries and data

Feature repositories depend on app-owned database/identity APIs. Room is the observable cache; the Supabase module implements remote operations and identity/session management. Only native secure storage, URL delivery and sharing are platform-specific. Keep Koin at composition boundaries and register feature-owned Nav3 serializers explicitly. Provider sessions/query builders/DTOs do not cross public contracts.

The server stores profiles, lists, active/inactive memberships, unique current list/movie entries, immutable film events, hashed invites and idempotency receipts. Exposed tables use RLS. RPCs validate authenticated identity and membership; private definer functions have fixed search paths and explicit grants. Film mutations and events commit together. Creator departure does not change permissions. Join and Leave serialize on the same list record.

Room 8→9 preserves profile/bookmarks/personal lists/movie enrichment and adds profile confirmation/color, shared snapshots, activity/cursors and already-started online operations. Versions 1–6 remain the only authorized destructive fallback. Shared cache removal does not delete independently referenced films/bookmarks.

## Synchronization and failure semantics

- Each list has a monotonic revision. Apply authoritative snapshots atomically; reject older snapshots. Realtime signals trigger refetching. Foreground/reconnect refresh and a 30-second foreground refresh recover missed messages. Polling is foreground-wide in v1 to cover pickers opened from any tab.
- Current membership is unique. No-op adds/removes produce no duplicate activity. Last server-committed change wins; an existing entry retains its original contributor until removed and added again.
- Persist a request ID before sending an online mutation. Unknown outcomes retain that exact request for reconciliation, including publication and Leave. Retried old additions cannot resurrect a film removed later. This is not an offline editing queue.
- Write completion means the server acknowledged the command and the latest snapshot was applied. Shared pending controls wait for that revision, not a requested boolean that a collaborator can immediately change again.
- Activity retains its server pagination cursor so offline gaps cannot be skipped by using the oldest cached event. Cache synthetic/remote summaries only when a movie is missing; do not replace richer/fresher local details with old audit text.
- Auth/storage failure never silently creates a replacement identity. Credential storage is Keystore-backed encryption on Android and Keychain on iOS. Sessions never enter Room, NavKeys, URLs or logs.
- Missing configuration leaves local functionality usable and explains how to configure sharing. Backend failures retain cache and expose retry. No silent demo-data fallback or private fork.

## Delivery and validation

Backend setup is documented in [Supabase setup](../../supabase/README.md). Commit reviewed migrations/templates, never live credentials or user data. A new Supabase instance needs its own deployment/settings; another provider implements the neutral APIs and bindings. Live provisioning is separate from feature implementation.

Run existing host tests, lint/debug packaging, unsigned physical-iOS builds, relevant Android test-APK compilation, and documentation checks. Maintain existing assertions/fixtures; new test cases remain deferred under the feature-phase policy. Disposable PostgreSQL checks verify allow/deny paths, actor attribution, deduplication/retries, invitation revocation and retirement. These do not establish deployed Supabase Auth/Realtime or native runtime acceptance.

People validate two physical devices: join/publish, bidirectional changes and conflicts, interruption/retry, offline/reconnect, activity/avatars, cold/warm links, leave/rejoin, persistence and existing local/bookmark/navigation behavior. Never run emulators/simulators or connected tests as an agent.

Deferred: recovery/device linking, color picker, feature tour, HTTPS links/install fallback, push/comments/voting, and expanded automated coverage. Public source is not permission to expose an unrestricted anonymous signup service: apply CAPTCHA/abuse controls before public backend distribution. Free projects may pause when inactive.

Research: [Spotify collaboration](https://support.spotify.com/au/article/collaborative-playlists/), [Apple Notes activity](https://support.apple.com/en-gb/guide/iphone/iphe4d04f674/ios), [anonymous authentication](https://supabase.com/docs/guides/auth/auth-anonymous), [RLS](https://supabase.com/docs/guides/database/postgres/row-level-security), [Android deep links](https://developer.android.com/training/app-links/create-deeplinks), [Apple URL schemes](https://developer.apple.com/documentation/xcode/defining-a-custom-url-scheme-for-your-app), [Supabase Kotlin 3.9](https://github.com/supabase/supabase-kt/releases/tag/3.9.0). Living documentation reviewed during planning.

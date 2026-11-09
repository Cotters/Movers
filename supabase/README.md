# Sharing backend setup

Movers works with local lists without backend configuration. Sharing requires your own Supabase project; no project is provisioned automatically. See the [feature design](../docs/features/shared-watchlists.md) and [public-source guidance](../docs/workflow/open-source.md).

## Deploy your own instance

1. Create a Supabase project, then enable **Anonymous Sign-Ins** in Auth settings. Keep signup rate limits enabled. This first version targets a private friend group; public distribution needs CAPTCHA integration/abuse protection first.
2. Apply every reviewed SQL file in `migrations/` in filename order to the new project's database. Use the Supabase migration workflow for an existing database; do not blindly rerun creation scripts. The local `config.toml` enables anonymous signup, exposes only public APIs, and seeds no users. CLI local development requires Docker; consult `supabase --help` and the current command's `--help` before use. Do not reset a live project to apply this feature.
3. Ensure `movers_list_signals` is in the `supabase_realtime` publication. The migration adds it when that publication exists. Keep `movers_private` out of the API's exposed schemas and extra search paths.
4. Configure clients with the project's HTTPS URL and **publishable key**. This key is visible in an app and is not an authorization boundary. Never put a secret/service-role key, database password or connection string in either client.

On Android append these placeholders, replaced locally, to ignored root `local.properties`:

```properties
supabase.url=https://YOUR_PROJECT.supabase.co
supabase.publishableKey=YOUR_PUBLISHABLE_KEY
```

On iOS use ignored `iosApp/Local.xcconfig`, following the tracked example. The `$()` in the URL prevents xcconfig treating `//` as a comment:

```text
SUPABASE_URL = https:/$()/YOUR_PROJECT.supabase.co
SUPABASE_PUBLISHABLE_KEY = YOUR_PUBLISHABLE_KEY
```

Rebuild each app after configuration changes. Empty settings retain local-only operation. Existing TMDB configuration is independent. No sign-in redirect URL/domain is needed: invite links use the app's `movers` scheme and explicit preview/Join.

## Access model and operational limits

Public invoker RPCs call scoped private definer implementations. Table writes, activity actors and timestamps are server-owned. Only active members can fetch list content/activity; valid-token previews return limited metadata. All members have the same permissions. No privileged key is needed by the app.

Operation receipts are retained to make retries safe; activity and departed participants retain attribution. Retired lists are inaccessible to clients. Anonymous sessions cannot recover after device credential loss; a remaining member can invite a new identity. Native cache remains readable offline and refreshes when connected. A Supabase Free project can pause after inactivity; restore the project if needed, then retry without deleting app data.

Before deployment acceptance, verify with disposable users: member/non-member/guest access; forged actor rejection; repeated requests and conflicting additions/removals; valid/expired/revoked links; creator departure; last-member retirement; and Auth refresh/Realtime reconnect. The feature's local PostgreSQL checks cover SQL behavior, not hosted-service or physical-device behavior. Use invented records only and keep raw sessions, invitation links and project diagnostics out of public logs.

Replacing Supabase means implementing `SharedWatchlistRemote` and `ParticipantIdentity`, then changing composition bindings. Preserve command idempotency, authorization, revisions and failure semantics. UI/repository code must not need provider-specific types. This is an adapter change, not merely changing a URL.

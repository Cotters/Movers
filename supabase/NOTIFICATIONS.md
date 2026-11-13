# Apple notification deployment

The [agreed behavior](../docs/features/watchlist-notifications.md) uses the existing backend plus APNs. Applying migrations or building the app does not activate dispatch. No notification keys belong in either app config.

## Prepare Apple and the backend

1. Apply reviewed migrations using the [existing backend helper](README.md). Notification tables/trigger are installed with enqueue disabled. Existing apps and RPC signatures remain compatible.
2. Enable Push Notifications for `com.jcotters.movers` and refresh signing profiles. ASC CLI supports capability/profile management: resolve the bundle resource with `asc bundle-ids list --identifier com.jcotters.movers`, then enable `PUSH_NOTIFICATIONS` using `asc bundle-ids capabilities add --bundle BUNDLE_RESOURCE_ID --capability PUSH_NOTIFICATIONS`. Check current capabilities first. Xcode automatic provisioning can regenerate profiles during a signed archive using `-allowProvisioningUpdates`; see the [existing ASC archive workflow](../docs/workflow/multiplatform.md#app-store-connect-and-uploads). Create the separate APNs service keys in the [Apple Developer Portal](https://developer.apple.com/help/account/keys/create-a-private-key/): the installed ASC CLI 5.1 does not expose APNs key creation, and its App Store Connect API keys cannot be reused for APNs. Prefer topic-specific keys scoped to this bundle, with separate sandbox/production environments. Development signing uses sandbox; TestFlight/App Store signing uses production. The app reads its signed entitlement and fails registration visibly if it is missing.
3. Configure the Edge Function secrets through the Supabase dashboard or CLI using ignored `supabase/.env.notifications.local`. Restrict its permissions to the owner (`chmod 600`) before adding values. Never paste keys into chat or command arguments. Discover installed CLI flags with `supabase secrets set --help` and `supabase functions deploy --help`. The platform supplies `SUPABASE_URL` and server-only `SUPABASE_SERVICE_ROLE_KEY` to the function.

| Secret | Value |
| --- | --- |
| `PUSH_ENABLED` | `false` initially; `true` enables the worker |
| `PUSH_DISPATCH_SECRET` | Independent random 256-bit or stronger secret |
| `APNS_ENVIRONMENTS` | `sandbox` initially; later `sandbox,production` or `production` |
| `APNS_TEAM_ID` | Apple developer team identifier |
| `APNS_SANDBOX_KEY_ID` | Sandbox signing key identifier |
| `APNS_SANDBOX_PRIVATE_KEY` | Sandbox `.p8` PEM contents, preserving newlines |
| `APNS_PRODUCTION_KEY_ID` | Production key identifier when enabled |
| `APNS_PRODUCTION_PRIVATE_KEY` | Production `.p8` PEM contents when enabled |

4. Deploy `watchlist-notifications`. Its JWT gateway check is disabled intentionally; the handler checks `PUSH_DISPATCH_SECRET` before any privileged operation. Public/publishable keys cannot dispatch notifications. Use only HTTPS and standard certificate validation. No client-side scheduling or Firebase is involved.
5. Add `movers_push_project_url` and `movers_push_dispatch_secret` to Supabase Vault using the dashboard; the latter must match the function secret. The scheduler stores only secret references. Run [notifications-schedule.sql](notifications-schedule.sql) after reviewing the target. This installs the named minute job using `pg_cron` and `pg_net`.

## Validate and activate

Use signed physical-device builds with distinct participant identities. First validate the native token/permission registration and the hosted function's HTTP/2 connectivity, JWT authentication and APNs acceptance in sandbox. Turn on `PUSH_ENABLED` and enable database enqueue only for this controlled acceptance:

```sql
update movers_private.push_settings set enabled=true where singleton;
```

Confirm ordinary additions/imports, five quiet minutes, cancellations, recipients and cold/warm taps. The sender must close Movers after server confirmation during acceptance. Verify denial/re-enabling in iOS Settings and test foreground banners/default sound. Then validate production with a signed TestFlight build and a production key before enabling production recipients. Delivery remains best effort: Focus, notification summaries, connectivity and permission settings affect presentation. A successful HTTP 200 from APNs means accepted, not displayed.

The dispatcher uses native fetch, not a Node APNs package or unstable Deno HTTP-client APIs. Hosted HTTP/2 and Apple signing cannot be proven by a local Deno typecheck. Do not mark deployment complete without APNs acceptance evidence. The local PostgreSQL verification uses synthetic Auth roles; it does not establish hosted Auth, Realtime or Vault operation.

## Observe, stop and recover

Inspect Cron run history and `net._http_response` for dispatch invocation failures; the HTTP response reports prepared groups/configuration failures. Function logs contain delivery IDs and fixed outcome/reason categories, never tokens or bodies. Private `push_batches`/`push_deliveries` contain due times, state, attempts, lease expiry and last error for server-side diagnosis. Review overdue work and 403/429/5xx categories; restrict dashboard access accordingly. Frozen groups/deliveries older than 30 days are removed during preparation; activity history is retained.

For a kill switch set `PUSH_ENABLED=false` and disable database enqueue:

```sql
update movers_private.push_settings set enabled=false where singleton;
```

Already accepted APNs alerts cannot be recalled. Pending work expires 24 hours after its due time; disabled periods create no new work. Re-enabling resumes unexpired existing work. Do not remove migrations or reset app identities to stop delivery. To stop scheduler invocations too, run `select cron.unschedule('movers-watchlist-notifications');` on the intended backend.

Rotate compromised signing keys in Apple and update the matching environment's secrets/key ID. Provider JWTs are privately cached for 30 minutes across function invocations; changing key ID invalidates the cache immediately. If replacing a key with the same identifier during diagnosis, delete only its environment's row from `movers_private.push_provider_tokens`. Never print that table. Fix credentials before retrying; do not treat authentication failures as bad recipient tokens.

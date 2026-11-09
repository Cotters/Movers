# Sharing backend setup

Movers works with local lists without backend configuration. Sharing requires your own Supabase project; no project is provisioned automatically. See the [feature design](../docs/features/shared-watchlists.md) and [public-source guidance](../docs/workflow/open-source.md).

## Run from a token-enabled zsh terminal

The helper uses the `SUPABASE_ACCESS_TOKEN` exported in your terminal. It refuses to fall back to the CLI's saved account. Python 3, curl, and Supabase CLI 2.81.3 or newer are required for remote commands. On macOS the helper uses `/usr/bin/curl` and system certificate trust, so an unconfigured Python CA bundle does not block HTTPS. Both ignored app configs must contain matching project URLs/publishable keys. The standard `https://PROJECT_REF.supabase.co` URL selects the target; credentials are never printed or embedded in command arguments.

```sh
./scripts/supabase-setup.sh check
./scripts/supabase-setup.sh inspect
./scripts/supabase-setup.sh deploy
./scripts/supabase-setup.sh verify
```

- `check` validates local config only; no token/network required.
- `inspect` is the default. It verifies account access and the app key, reports Anonymous Sign-Ins, shows schema/RLS metadata and migration history, and previews pending migrations. It applies no migrations or Auth changes.
- `deploy` previews then applies versioned migrations through the CLI, enables only Anonymous Sign-Ins through the Management API, and runs deployment verification. Review the target and CLI migration confirmation. It never resets a database or pushes the entire local Auth configuration. If signup is disabled globally, enable it in Auth settings first.
- `verify` checks tables/RLS, invoker RPCs, client grants, Realtime publication, anonymous signup and security advisors. It creates no users/lists and is not a substitute for collaboration/device acceptance.

HTTP credentials are passed to curl through stdin, not process arguments or files. The helper disables curl startup config, keeps TLS certificate verification enabled, and reports DNS, timeout, TLS, HTTP and JSON failures separately without printing raw responses.

CLI linking happens in a private temporary directory, cleaned up on exit; existing project links and saved account login remain unchanged. If asked for a database password, enter it locally (or leave blank to skip link-time validation). CLI push may require database connectivity/password; never paste those credentials into chat. Security advisors fail on errors; review any warnings separately. If deployment partially completes, retry `deploy`: CLI migration history prevents reapplying completed migrations, and enabling anonymous signup is idempotent.

If the token is not yet exported, enter the **Movers account personal access token** locally using zsh's hidden input:

```zsh
read -rs "SUPABASE_ACCESS_TOKEN?Movers access token: "
printf '\n'
export SUPABASE_ACCESS_TOKEN
```

Do not put personal access tokens in app configs or tracked files. Closing that terminal or unsetting the variable restores the CLI's saved account behavior. An agent's shell does not inherit exports from your terminal. Helpers can run from another current directory using their absolute path. Share only sanitized output if a command fails.

The helper changes Auth using the documented [Management API](https://supabase.com/docs/reference/api/v1-update-auth-service-config); migrations and advisors use the [CLI](https://supabase.com/docs/reference/cli/introduction). If access is denied, check that the token's account/permissions cover this project; do not switch the CLI's global account as a workaround.

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

Rebuild each app after configuration changes. Empty settings retain local-only operation. Existing TMDB configuration is independent. No sign-in redirect URL/domain is needed: opening an invite through the app's `movers` scheme joins immediately; pasted links use one Join action.

## Access model and operational limits

Public invoker RPCs call scoped private definer implementations. Table writes, activity actors and timestamps are server-owned. Only active members can fetch list content/activity; valid-token previews return limited metadata. All members have the same permissions. No privileged key is needed by the app.

Operation receipts are retained to make retries safe; activity and departed participants retain attribution. Retired lists are inaccessible to clients. Anonymous sessions cannot recover after device credential loss; a remaining member can invite a new identity. Native cache remains readable offline and refreshes when connected. A Supabase Free project can pause after inactivity; restore the project if needed, then retry without deleting app data.

Before deployment acceptance, verify with disposable users: member/non-member/guest access; forged actor rejection; repeated requests and conflicting additions/removals; valid/expired/revoked links; creator departure; last-member retirement; and Auth refresh/Realtime reconnect. The feature's local PostgreSQL checks cover SQL behavior, not hosted-service or physical-device behavior. Use invented records only and keep raw sessions, invitation links and project diagnostics out of public logs.

Replacing Supabase means implementing `SharedWatchlistRemote` and `ParticipantIdentity`, then changing composition bindings. Preserve command idempotency, authorization, revisions and failure semantics. UI/repository code must not need provider-specific types. This is an adapter change, not merely changing a URL.

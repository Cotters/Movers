# Public source and replaceable backends

Reviewed: 8 November 2026. Movers is a resume/portfolio demo intended for public reuse. Treat tracked code, documentation, fixtures, build output, and CI logs as public. Local watchlists implement both boundaries below with Room; no Supabase or remote backend integration is implemented.

## Two explicit boundaries

Use the existing repository, contract/internal, and Koin patterns:

```text
Screen / ViewModel / use case
            |
Feature repository contract (app operations and models)
            |
Repository implementation (data policy, mapping, cache/sync)
            |
Vendor-neutral database API (typed feature-data operations)
            |
Supabase adapter or another backend adapter
```

The user explicitly requires the database API beneath repositories as a second seam. Keep it focused on the data needed by the feature. Model list observation and writes as typed operations with defined results and failure semantics; agree on exact signatures during Shared Lists planning. Avoid exposing SDK query builders, table names, vendor DTOs, authentication sessions, or Supabase exception types through either public boundary.

Supabase-specific clients and classes belong in the adapter implementation. Koin selects the implementation at the composition boundary. A replacement provides the same database contract and changes the binding/configuration; screens and use cases stay unchanged. Define observation, cancellation, ordering, write outcomes, and authorization expectations so fake and real adapters mean the same thing. App-owned identity/session contracts must also support replacing the auth provider; a database-only interface cannot make provider-specific auth portable.

This applies [Android's repository/data-source separation](https://developer.android.com/topic/architecture/data-layer) to the user's portability requirement. Extend useful existing seams; do not create a universal database framework or rewrite unrelated modules just to add layers.

The local implementation uses `IWatchlistsRepository` for app operations, `WatchlistDatabase` for provider-neutral storage operations, and internal `RoomWatchlistDatabase` selected by Koin. Public signatures expose Kotlin/app models and flows, without Room, SQL, or provider types. Multi-list writes commit atomically and are idempotent; observation owns membership state. Replacing storage requires implementing this contract and changing the binding. Collaboration still needs its own authorization, synchronization, and backend plan.

## Reproducible setup and private deployment

| Safe public project material | Keep out of tracked files and public output |
| --- | --- |
| Database API contracts, Supabase adapter source, sanitized configuration templates, setup instructions | Real user records, account exports, database dumps, backups, and private diagnostic data |
| Reviewed schema migrations, access policies, functions needed by the feature, synthetic seed data, policy tests | Database passwords/connection strings, secret/service-role keys, OAuth client secrets, access/refresh tokens, signing credentials |

Publishing reviewed schema and migration code lets someone build their own instance. It does not grant access to the maintainer's database. Security must hold when the schema and client code are known: Supabase's [RLS guidance](https://supabase.com/docs/guides/database/postgres/row-level-security) is the basis for membership policies and allow/deny tests.

Supply deployment settings through ignored local configuration or CI secret storage, with placeholder-only examples and clear setup errors. Keep the maintainer's live project configuration out of samples and default builds. Supabase URLs and publishable keys are visible in a configured client/APK, so they cannot be a security boundary; privileged keys never belong in the app. See [API key guidance](https://supabase.com/docs/guides/getting-started/api-keys).

Version reviewed migrations, non-sensitive configuration, and setup steps together. Seed only invented demo data. A contributor should be able to create an empty instance, apply the supplied migrations/policies, configure their client, and verify the documented behavior. Supabase documents [versioned local development](https://supabase.com/docs/guides/local-development/cli-workflows) and [reproducible seeding](https://supabase.com/docs/guides/local-development/seeding-your-database). A containerized local database is host-side infrastructure; it does not authorize Android emulator/simulator use.

## What “plug and play” means

- **Another Supabase instance:** apply the versioned backend setup and replace local URL/publishable-key settings. It must not depend on the maintainer's deployment or accounts.
- **Another backend/database:** implement the neutral database API, supply compatible authentication/session behavior where needed, and replace Koin bindings/configuration. This needs an adapter and is not a credentials-only switch.
- **Portfolio evaluation without an account:** plan an explicit demo adapter with synthetic fixtures and visible demo status, using the same contracts. This remains a proposed capability for Shared Lists planning; do not silently substitute fake data when a real backend fails.

Before calling the integration portable, run the same behavior/contract checks against its fake and real adapters and verify access policies for members, non-members, guests, and removed members. Routine host checks must not require access to the maintainer's backend. Do not add backend deployment to PR CI implicitly. Review tracked changes and relevant history for accidental credentials before publication; adding an ignore rule alone does not remove previously committed values.

No project license was found during this review. Choosing a license remains a maintainer decision before advertising licensed reuse; do not invent a license or copyright ownership. See [GitHub's license guidance](https://choosealicense.com/no-permission/).

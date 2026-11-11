# List import and export

Agreed and implemented 11 November 2026. Shared Android/iOS UI; provider-neutral lookup/storage contracts and native document adapters. Current evidence and deployment state live in the [handoff](../workflow/handoff.md).

## Flow

Every personal/shared list offers **Import films** and **Export list** in its menu. Import accepts file selection or pasted text. A first-use inline introduction has Got it/Skip; dismissal is stored locally and Help reopens it. Selecting a source only parses it. A small preview, detected format and optional format/header/column controls precede Import.

Session progress/report stay in the entry-scoped ViewModel, not Room or saved navigation. Stop/background/tab changes pause unsent work; Resume/Retry continues while the entry survives. Leaving/restarting discards the report, retaining confirmed membership. Already-started shared commands remain subject to durable reconciliation and may finish independently.

Results show imported/skipped/attention counts. The default filter shows remaining rows with short reasons. Find film uses the same shared search field and result component as full Search, in a compact sheet with posters, years and synopsis. Typing searches after a short debounce. Selecting a film imports it immediately. Save remaining produces a CSV of unresolved/failed/unprocessed references and reasons for later repair/re-import.

## Formats and matching

- Individual Letterboxd/IMDb CSVs, generic CSV/TSV, semicolon/pipe/newline text, pasted `Title (Year)` and recognised TMDB/IMDb film links. No ZIP or XLSX support.
- UTF-8 with optional BOM and BOM-marked UTF-16. Sources are bounded to 5 MiB and 10,000 entries, with explicit errors rather than silent truncation. CSV parsing is quote-aware, including escaped quotes/embedded newlines. Broken sections preserve the readable prefix and surface the unreadable remainder.
- Recognise title/name, year, TMDB ID and IMDb Const/ID columns; ignore account metadata/reviews. Known non-film rows are skipped with a reason. Numeric titles such as 1917 remain titles unless explicitly identified as IDs.
- Explicit IDs take priority. **Latest user decision supersedes the original strict matching plan:** otherwise use the first valid search result, with year filtering first and a retry without the year when empty. Empty results need attention; contradictory explicit IDs still require selection. Two concurrent lookups, per-session memoization and bounded rate-limit retries avoid flooding the service. Widespread lookup/connectivity failure pauses remaining work.
- Deduplicate by canonical movie ID against list membership and the import; preserve distinct remakes. Skipped membership never changes its contributor or creates activity.

Export captures the displayed list snapshot without network access. CSV contains `Title,Year,tmdbID`, with quoted/spreadsheet-safe cells; text contains one `Title (Year)` per line. JSON uses `{format:"movers.watchlist",version:1,name,films:[{tmdbId,title,year}]}` and is accepted on re-import. No contributors, sessions, activity, invitations or backend IDs are exported. Native Save/Share report cancellation/errors without claiming a share sheet saved a file.

## Ownership and writes

`MovieImportResolver` owns provider-neutral references/results; TMDB search, external-ID lookup, caching and mapping remain in movie internal. Search summaries persist before membership writes without replacing enrichment. Parsing/encoding and import coordination are separate from Compose rendering. `FilmImportKey` stores only list/entry identity and is registered explicitly; feature Koin owns its ViewModel.

Repository → neutral `WatchlistDatabase` → Room/collaborative adapter imports batches of up to 50 immutable film snapshots. Local membership commits transactionally. Shared imports use `SharedCommand.ImportFilms` and `movers_import`, returning per-film Imported/AlreadyPresent/Rejected outcomes and revision. Request IDs and payloads remain identical on retry; server receipts retain original outcomes even after later collaborator removals. Incomplete acknowledgements remain pending. Remote acknowledgements are projected through the existing Room cache before success.

The additive migration extends receipt data and private command logic, retaining ordinary RPC signatures. Import validates active membership, derives actor/time on the server, inserts only absent membership and emits imported activity atomically. No-op/rejected rows create no activity. Removals stay Removed; normal catalogue/search/picker additions stay Added. Manual resolution inside import stays Imported. Attribution now says Imported rather than Imported when shared. Publishing an existing personal list keeps its existing import semantics; historical actors/dates are never invented from files.

No Room schema change or persisted import-job table. Android uses document intents and a non-exported FileProvider restricted to the export cache. iOS uses copied documents and a retained Objective-C delegate separate from the Kotlin actions interface. Native handles stay out of shared state. Imported file contents are not logged/uploaded; temporary copies/exports are cleaned up within platform ownership constraints.

## Deployment and acceptance

Existing hosted projects need `20261110155745_film_import_batches.sql`; see [backend setup](../../supabase/README.md). Deployment is a separate user action. Older ordinary clients remain compatible. Missing import RPCs report a concise unavailable error; they never fall back to ordinary Added writes.

Maintain existing tests/fixtures; no new cases in the feature phase. Physical Android/iPhone checks remain user-owned: local/cloud files, paste/quotes, format overrides, no-match rows, duplicate/remake handling, first-result matching, compact poster search, Stop/Resume, save/share cancellation, export round-trips and two-identity attribution/retry. Broader parser/import/native coverage belongs in post-feature review. No emulator/simulator/connected tests by agents.

Research: [CSV quoting](https://www.rfc-editor.org/rfc/rfc4180.html), [TMDB finding data](https://developer.themoviedb.org/docs/finding-data), [rate limits](https://developer.themoviedb.org/docs/rate-limiting), [Android documents](https://developer.android.com/training/data-storage/shared/documents-files), [Apple document picker](https://developer.apple.com/documentation/uikit/uidocumentpickerviewcontroller), [Supabase functions](https://supabase.com/docs/guides/database/functions). Living API guidance checked during planning/implementation; installed Kotlin/Native bindings supplied exact native signatures.

# Current handoff

Updated 13 November 2026. Cleanup starts from `364b988`; owner authorized cleanup and including existing working changes. Prior feature/verification detail: [pre-cleanup handoff](history/2026-11-12-before-lint-cleanup.md).

## Active work: Kotlin hygiene cleanup

[Cleanup plan](kotlin-lint-cleanup.md) is in progress: **656 initial findings → 55 remaining**. Strict source lint still fails; no baseline, broad suppression, ignore-failure setting or relaxed application complexity limits. Intermediate cleanup commits are explicitly not lint-clean feature commits.

Completed slices cover actual Studio formatting, declaration/syntax cleanup, document codecs, named limits, shared-control responsibilities, membership waits and existing migration fixture organization. The owner's title typography was included as authorized in `74dd5fb`. Later commits and evidence are in the cleanup plan/Git.

Latest slice gives invitation joining its own Nav-entry-owned `JoinWatchlistViewModel` and Koin binding, leaving existing-list sharing/activity in `SharingViewModel`. Add-to-list choices/form, pinned list groups, shared feedback and gallery examples are focused render sections with existing callbacks/layout. No visual redesign, new tests, dependencies or backend/schema changes.

The Studio formatter works alongside the user's IDE using isolated ignored directories in `build/quality/studio-formatter`; serialize its invocations. Explicit `STUDIO_PROPERTIES` overrides remain supported. Canonical code gate is `scripts/check-code.sh 364b988`; inspect exact staged content and report remaining lint failure. Type-aware IDE inspections are unavailable; source-only lint cannot prove unused imports or all compiler warnings absent.

## Behavior to preserve

- Full Add/Pin swipes arm at 65% of row width (at least two action widths) and act on release. Short gestures reveal; Remove is reveal-and-tap with delayed-write Undo. Cancellation/disabled actions cannot fire. [Swipe design](../features/swipe-actions.md).
- Posters retain Home ×1.33 / watchlist ×1.20 widths, 2:3 ratios and bottom alignment; contributor avatars use bottom-right footers below details with attribution taps. Keep the owner's title typography.
- Sorting, five local pins and film-search suggestions retain saved/query/scroll state. Preserve cancellation, installation identity, persisted relationships, failure/retry handling and Room migrations. [Organization](../features/watchlist-organization.md), [shared lists](../features/shared-watchlists.md).

## Latest verification and next action

- Joining/small-screen slice: nine Kotlin files pass Studio formatting; mechanical checks pass; strict source lint reports 55 remaining. Affected watchlist/styleguide/shared host tasks pass. No new cases or devices.
- Recoverable-error slice (`3376203`) passed the complete existing Android host suite. `recoverableResult` rethrows cancellation/fatal errors; caller messages/mapping and `finally` cleanup stay local. Returned cancellation failures also propagate at detail/removal boundaries.
- DAO/cache slice previously passed generated Room compilation and affected host suites; exported schema is unchanged. Compiler still emits existing library/beta/deprecation warnings; no claim of zero type-aware warnings.
- Document parser has no dedicated existing test suite; compilation/source review are the evidence, not runtime edge-case coverage. Expansion remains deferred by owner policy.
- Next: simplify error boundaries and remaining oversized/complex UI/import functions, finish genuine findings, verify cumulative Android/iOS compilation, then enforce the clean profile in CI. Existing missing `iosApp/MoversApp.swift` remains an app-link prerequisite; preserve its earlier deletion. Physical Android/iPhone acceptance stays user-owned; never use emulators/simulators.

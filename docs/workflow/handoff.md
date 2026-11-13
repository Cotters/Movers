# Current handoff

Updated 13 November 2026. Cleanup starts from `364b988`; owner explicitly authorized cleanup and including existing working changes. Prior feature/verification detail: [pre-cleanup handoff](history/2026-11-12-before-lint-cleanup.md).

## Active work: Kotlin hygiene cleanup

[Cleanup plan](kotlin-lint-cleanup.md) is in progress. The starting strict Detekt scan had 656 findings across 129 files. First slice formats 106 lint-flagged files with Android Studio; a Kotlin lexer comparison confirms unchanged code/string tokens in those files. Owner's staged film title/style edits are included as authorized, not reverted.

The formatter now uses isolated ignored config/cache/log directories in `build/quality/studio-formatter`; an actual sample write succeeded while leaving the user's Studio open. Explicit `STUDIO_PROPERTIES` remains supported. This supersedes the earlier instance-lock blocker. Formatter invocations remain sequential.

Reviewed profile corrections extend default test exclusions to Android KMP host/device sources, permit `@Test` scenario names, avoid counting required interface overrides as independent methods, allow early guards and readability parentheses. Application complexity limits remain unchanged. No baseline, broad suppression or ignore-failure setting.

First cleanup commit: `74dd5fb`. The next slice moves value types to matching files, fixes constant names/redundant syntax and keeps mapper validation as explicit guards. Conditional callback lambdas retain their function type; a first compile caught and corrected a semantic parentheses-removal mistake.

Latest source-only scan: **157 findings remain**. Remaining slices cover actual long/complex functions, redundant syntax/naming, production constants and error boundaries. Do not present intermediate cleanup commits as lint-clean. Canonical pre-commit command is `scripts/check-code.sh`; formatting/import/diagnostic and manual architecture review remain separate requirements. See [code review](code-review.md).

## Behavior to preserve

- Full Add/Pin swipes arm at 65% of row width (at least two action widths) and act on release. Short gestures reveal; Remove is always reveal-and-tap with existing delayed-write Undo. Cancellation/disabled actions cannot fire. [Swipe design](../features/swipe-actions.md).
- Posters retain Home ×1.33 / watchlist ×1.20 widths, 2:3 ratios and bottom alignment; contributor avatars use agreed bottom-right footers below details with attribution taps. Keep the owner's title typography.
- Sorting, five local pins and film-search suggestions retain saved/query/scroll state. Shared providers remain behind neutral contracts; preserve cancellation, identities, persisted relationships, failure/retry handling and Room migrations. [Organization](../features/watchlist-organization.md), [shared lists](../features/shared-watchlists.md).

## Verification and next action

- Formatting slice: Studio format plus lexical token comparison. Naming/syntax slice: all 33 changed/new Kotlin files pass Studio formatting; mechanical checks pass, source lint still fails with 182 findings. Existing `testAndroidHostTest testDebugUnitTest` now succeeds after correcting conditional lambdas. No new cases or devices. Type-aware IDE inspections remain unavailable; compilation covers Android types, not native targets.
- Previous app evidence predates recent UI changes: `91bed7e`, 79 existing tests / 21 suites, Android debug build and shared iOS arm64 compilation. It is not evidence for forthcoming refactors. Existing missing `iosApp/MoversApp.swift` is still an app-link prerequisite; preserve its earlier deletion.
- Document slice: decoding, CSV state/mapping, film references and export codecs are separated. All eight changed Kotlin files pass Studio formatting; mechanical checks pass. Existing watchlist/shared host tests pass. No dedicated import-parser tests exist; edge cases received source review, not a claim of runtime coverage.
- Next: fix focused remaining findings, run existing host/native checks when the refactor scope warrants it (explain first), reach a clean agreed profile, then wire CI. New tests remain deferred. Physical Android/iPhone acceptance stays user-owned; never use emulators/simulators.

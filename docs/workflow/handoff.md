# Current handoff

Updated 13 November 2026. **Kotlin source cleanup complete: 656 findings → zero** for the reviewed strict profile. Task base: `364b988`. Owner authorized cleanup and including working changes; the earlier title typography and latest catalogue opt-in removal are preserved. [Cleanup evidence/history](kotlin-lint-cleanup.md), [prior feature handoff](history/2026-11-12-before-lint-cleanup.md).

## Current workflow and structure

- Before code commits: Studio formatting/import/diagnostic review, `scripts/check-code.sh`, full-function/caller and architecture review. Use the task-start commit after making task commits. [Code review](code-review.md).
- CI now independently runs strict public-source Kotlin lint; Warning/Error findings fail it and reports persist after failure. No baseline, broad suppression or relaxed complexity thresholds. Exact factory naming follows official CMP interop guidance. Hosted CI has not run locally.
- Parsing, cached film lookup, import receipt lifecycle, sorting and suggestions have focused models under the existing ViewModel scope/state. Shared cache/operation/activity/membership persistence is segregated without changing Room schema or transaction ownership. Feature-private route sections bind existing models/navigation; reusable controls retain value/event APIs.
- `recoverableResult` propagates cancellation and fatal errors; callers retain domain mapping and `finally` cleanup. Returned cancellation failures also propagate at detail/removal boundaries. No new dependencies, backend deployment or feature behavior scope.

## Latest verification

- `scripts/check-code.sh 364b988`: mechanical checks and source-only Detekt pass with zero findings. Studio validates changed Kotlin files; live IDE inspections remain unavailable.
- `scripts/verify.sh`: convention checks, existing host suites, Android lint and debug packaging pass. Removing obsolete Android resources was followed by `lintDebug :app:assembleDebug`; the APK retains all shared navigation icons. Android lint has **16 target/dependency advisories**, no code/resource findings. Room's expect/actual Beta notices remain on fresh compilation.
- `:shared:linkDebugFrameworkIosArm64`: physical-iOS framework links, including the working catalogue edit. Required NSData opt-ins are explicit. Full Xcode host build remains unverified because pre-existing `iosApp/MoversApp.swift` is absent; preserve its earlier deletion.
- `:shared:assembleAndroidDeviceTest :movie:internal:assembleAndroidDeviceTest`: existing test APKs compile only. No instrumentation/device/simulator execution or new cases. Dedicated import/parser coverage remains deferred; broader existing suites do not prove those edge cases.
- Workflow YAML parses; hosted CI and physical Android/iPhone visual acceptance remain pending.

## Preserve and next action

Preserve Add/Pin full swipes and tap-only Remove with delayed-write Undo; original pending receipts/credit/activity; Home ×1.33 / watchlist ×1.20 posters, 2:3 ratios and bottom alignment; bottom-right contributor footers; saved queries/sorts/pins; cancellation, installation identity and preserving Room migrations. [Swipe behavior](../features/swipe-actions.md), [organization](../features/watchlist-organization.md), [shared lists](../features/shared-watchlists.md).

Read the first unchecked private Todo and agree the next feature scope. Compatibility advisories are separate maintenance work. People verify watchlist menu/edit/search, short-list padding, poster/avatar layout and swipe Undo on physical devices; agents never launch emulators/simulators. Keep private Notes/config/logs untracked and preserve Human.md feedback.

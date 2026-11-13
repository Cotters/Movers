# Current handoff

Updated 13 November 2026. **Full iOS Debug build and unsigned Release archive verified; minimum iOS 16.0.** Current task base: `b007aa9`. Kotlin source cleanup remains complete: **656 findings → zero**, with working title/catalogue edits preserved. [Cleanup evidence/history](../kotlin-lint-cleanup.md), [prior feature handoff](2026-11-12-before-lint-cleanup.md).

## Current workflow and structure

- Before code commits: Studio formatting/import/diagnostic review, `scripts/check-code.sh`, full-function/caller and architecture review. Use the task-start commit after making task commits. [Code review](../code-review.md).
- CI now independently runs strict public-source Kotlin lint; Warning/Error findings fail it and reports persist after failure. No baseline, broad suppression or relaxed complexity thresholds. Exact factory naming follows official CMP interop guidance. Hosted CI has not run locally.
- Parsing, cached film lookup, import receipt lifecycle, sorting and suggestions have focused models under the existing ViewModel scope/state. Shared cache/operation/activity/membership persistence is segregated without changing Room schema or transaction ownership. Feature-private route sections bind existing models/navigation; reusable controls retain value/event APIs.
- `recoverableResult` propagates cancellation and fatal errors; callers retain domain mapping and `finally` cleanup. Returned cancellation failures also propagate at detail/removal boundaries. No new dependencies, backend deployment or feature behavior scope.

## iOS host repair and workflow

- `MoversApp.swift` was deleted in swipe commit `60b3c27` while Xcode/XcodeGen retained its source reference. Restored the previous entry point, shared-service initialization, Compose embedding and invitation URL forwarding. No shared behavior or credentials changed. The earlier missing-host limitation is resolved.
- Xcode Debug/Release and XcodeGen now specify iOS 16.0. Keep both aligned. The Swift formatter has tracked four-space/120-character settings. `verify-ios.sh` validates the entry point, SDK, JDK 17 and Swift formatting before the full host build; it never installs/launches.
- [iOS build/release procedure](../ios-release.md) distinguishes unsigned compilation/archive, physical acceptance, signed delivery, ASC processing and TestFlight availability. A framework link alone is insufficient host verification. No signing, version bump, upload or hosted iOS CI was performed.

## Latest verification

- `scripts/check-code.sh b007aa9`: mechanical checks and source-only Detekt pass with zero findings. Strict Swift formatting, shell syntax, plist/XcodeGen configuration and documentation checks pass; invalid arguments/JAVA_HOME fail before compilation. No Kotlin edits in the host-repair task; cleanup Studio checks remain recorded in its evidence, with live IDE inspections unavailable.
- `scripts/verify.sh`: convention checks, existing host suites, Android lint and debug packaging pass. Removing obsolete Android resources was followed by `lintDebug :app:assembleDebug`; the APK retains all shared navigation icons. Android lint has **16 target/dependency advisories**, no code/resource findings. Room's expect/actual Beta notices remain on fresh compilation.
- `scripts/verify-ios.sh` and `--archive`: complete unsigned physical-device Debug app and Release archive pass on Xcode 26.3 / iOS SDK 26.2 / JDK 17, including native Release optimization. Both logs emit no warnings/errors. Debug and archive app metadata plus Mach-O show minimum 16.0; archive contains Compose resources, compiled assets, privacy manifest and app dSYM. Version/build stay 0.2.0 (3); signing and runtime were not verified. Local logs: `movers-ios-host-debug.log` and `movers-ios-host-archive.log` under the system temporary directory.
- `:shared:assembleAndroidDeviceTest :movie:internal:assembleAndroidDeviceTest`: existing test APKs compile only. No instrumentation/device/simulator execution or new cases. Dedicated import/parser coverage remains deferred; broader existing suites do not prove those edge cases.
- Workflow YAML parses; hosted CI and physical Android/iPhone visual acceptance remain pending.

## Preserve and next action

Preserve Add/Pin full swipes and tap-only Remove with delayed-write Undo; original pending receipts/credit/activity; Home ×1.33 / watchlist ×1.20 posters, 2:3 ratios and bottom alignment; bottom-right contributor footers; saved queries/sorts/pins; cancellation, installation identity and preserving Room migrations. [Swipe behavior](../../features/swipe-actions.md), [organization](../../features/watchlist-organization.md), [shared lists](../../features/shared-watchlists.md).

Owner should rebuild/run in Xcode on a physical iPhone and verify launch, invitations, keyboard/navigation and relaunch persistence. iOS 16 device acceptance remains pending; newer-device success cannot prove minimum-OS compatibility. For a new task, read the first unchecked private Todo and agree scope. Compatibility advisories are separate maintenance work. People verify watchlist menu/edit/search, short-list padding, poster/avatar layout and swipe Undo on physical devices; agents never launch emulators/simulators. Keep private Notes/config/logs untracked and preserve Human.md feedback.

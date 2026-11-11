# Movers development style guide

Approved 11 November 2026. Build one curated cinematic component playground before changing production UI. References: [Sequel](https://www.getsequel.app/), [Linear](https://linear.app/features), [RikkaUI](https://github.com/rainxchzed/RikkaUi) and [shadcn-multiplatform](https://github.com/jershell/shadcn-multiplatform).

## Ownership and access

- `:designsystem` owns experimental shared tokens/components; `:features:styleguide` and its contract own the gallery/NavKey. Existing convention plugins apply. The production theme is unchanged.
- Profile exposes Developer tools → Style guide only in debuggable Android apps or debug Kotlin/Native binaries. The route is guarded too. Its serializer remains registered so restored development navigation can be safely popped in Release/TestFlight. There is no runtime unlock setting or external gallery link.
- Gallery state is local/session-only. No repository, backend, account or real list actions. Haze is deferred, including its dependency; the full redesign waits for review of this guide.

## Delivery

1. Development gate, navigation and module boundaries.
2. Theme tokens, Inter font and basic controls over selected MIT-licensed Compose Unstyled 2.10.2 modules.
3. Working overlays and synthetic film-component examples.
4. Existing host/native checks, source/notice review and concise handoff/Todo updates.

Research: [Unstyled installation](https://composeunstyled.com/docs/getting-started/installation/), [Android debuggable flag](https://developer.android.com/reference/android/content/pm/ApplicationInfo#FLAG_DEBUGGABLE), [Native debug binary](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.native/-platform/is-debug-binary.html). No simulator/device interaction or new test cases. Physical acceptance remains user-owned.

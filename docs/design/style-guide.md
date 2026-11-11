# Movers development style guide

One curated cinematic component playground, approved and implemented 11 November 2026. Open **Profile → Developer tools → Style guide** in a Debug build. Review these components before redesigning production screens.

## What to try

| Section | Examples |
| --- | --- |
| Basics | Inter typography, graphite/ivory/mint palettes, original stroke icons and spacing |
| Controls | Primary/secondary/quiet/destructive and disabled/loading buttons, fields/search/multiline input, error/focus states and selections |
| Overlays | Keyboard-aware menu, dismissible dialog/sheet, rename input, Undo and retry feedback |
| Films | Synthetic posters/cards/rows, fractional rating, contributor initials, shared-list summary, activity, empty state and immediate multi-list selection |

**Adjust style** changes only the preview area: System/Light/Dark, 8/12/16 dp corners, Compact/Comfortable spacing. Defaults are System, 12 dp and Comfortable. Reset restores defaults and demo inputs while keeping the selected section. Settings live in the entry ViewModel; demo inputs/scroll are saved per section. They are not application preferences. Relaunch/process restoration is not durable design configuration.

Film examples use invented titles/participants and original drawn artwork, with independent membership per film. No images are downloaded and no repository/backend/real-list actions are performed by the gallery. Modal titles/content stay stable while closing. Existing added/red and removed/green activity semantics are retained in the samples.

## Ownership and access

- `:designsystem` owns shared experimental tokens/components; `:features:styleguide` and its contract own gallery/NavKey/entry state. Modules use existing conventions. No new platform target, database or toolchain migration.
- Production screens keep their existing Material theme and behaviours. The guide's outer development chrome also retains it; the examples have an isolated MoversDesignTheme. Do not make experimental components the production default until the guide review and adoption plan.
- Android eligibility uses the installed application's debuggable flag; iOS uses Kotlin/Native's debug-binary flag. Release/TestFlight have no launcher or runtime unlock. The app sanitizes all restored guide keys before providers run and rejects release navigation attempts; the entry is independently guarded. Its serializer remains available for safe restoration.
- This is a visibility/access boundary, not a promise to strip all guide code/assets from release packages. Backend and personal configuration remain private as before.

## Components and sources

Edit palettes/type/spacing in [MoversDesignTheme](../../designsystem/src/commonMain/kotlin/com/jcotters/designsystem/MoversDesignTheme.kt), defaults in [MoversStyleSettings](../../designsystem/src/commonMain/kotlin/com/jcotters/designsystem/MoversStyleSettings.kt), and component wrappers alongside them. Gallery stories live in the style-guide feature. Fields use state-based Foundation text input through Unstyled; wrappers retain labels, IME actions, paste/selection, focus and error semantics. Controls retain at least 48 dp touch targets; content wraps rather than relying on fixed text heights.

The style combines references from [Sequel](https://www.getsequel.app/), [Linear](https://linear.app/features), [RikkaUI](https://github.com/rainxchzed/RikkaUi) and [shadcn-multiplatform](https://github.com/jershell/shadcn-multiplatform). Wrappers/icons/artwork are original; no component source or visual assets were copied from those projects. See [provenance and bundled notices](../../designsystem/SOURCES.md).

Selected **Compose Unstyled 2.10.2** modules supply button/field/menu/dialog/sheet mechanics. Exact APIs were checked against Maven source jars and [official documentation](https://composeunstyled.com/docs/getting-started/installation/). Inter **v4.1** uses three static weights with its OFL licence. Both licences are shared resources and confirmed in the debug APK. Fonts total 1,248,684 raw bytes / 636,480 compressed APK bytes, not a whole application size delta.

**Haze is deferred**, including its dependency. Add a small effects gallery later, measure on physical devices and account for native-player/older-Android fallbacks before production adoption. Full app redesign remains a separate next phase.

## Validation

Existing shared host suite: 19 tests / 5 suites passed; existing navigation serialization assertion updated, no new cases. Android debug packaging and shared iOS arm64 source compilation passed. Source/diff/docs and basic contrast/target-size checks completed. Latest evidence lives in the [handoff](../workflow/handoff.md).

No simulator/device use or native visual acceptance is claimed. Physical review: development/release gating and restored stacks, theme isolation, large text, field focus/paste/IME, menu keyboard controls, sheet dragging/keyboard/dismissal, modal transitions, section/Reset retention and per-film demo memberships. The missing iOS Swift entry is still a separate app-build prerequisite. Expanded automated coverage remains post-feature work.

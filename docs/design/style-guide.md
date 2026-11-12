# Movers development style guide

The approved Movers design system, shared by Android and iOS. The owner reviewed the gallery and selected **16 dp corners and Comfortable spacing**. Open **Profile → Developer tools → Style guide** in a Debug build to review changes before applying them to feature screens.

## What to try

| Section | Examples |
| --- | --- |
| Basics | Inter typography, graphite/ivory/mint palettes, original stroke icons and spacing |
| Controls | Primary/secondary/quiet/destructive and disabled/loading buttons, fields/search/multiline input, error/focus states and selections |
| Overlays | Keyboard-aware menu, dismissible dialog/sheet, rename input, Undo and retry feedback |
| Films | Synthetic posters/cards/rows, fractional rating, contributor initials, shared-list summary, activity, empty state and immediate multi-list selection |

**Adjust style** changes only the preview area: System/Light/Dark, 8/12/16 dp corners, Compact/Comfortable spacing. Defaults are System, 16 dp and Comfortable. Reset restores defaults and demo inputs while keeping the selected section. Settings live in the entry ViewModel; demo inputs/scroll are saved per section. They are not application preferences. Relaunch/process restoration is not durable design configuration.

Film examples use invented titles/participants and original drawn artwork, with independent membership per film. No images are downloaded and no repository/backend/real-list actions are performed by the gallery. Modal titles/content stay stable while closing. Activity uses green for added/imported and red for removed, with italic film titles. Production and gallery use the same semantic colour tokens.

## Ownership and access

- `:designsystem` owns shared production tokens/components; `:features:styleguide` and its contract own gallery/NavKey/entry state. Modules use existing conventions. No new platform target, database or toolchain migration.
- Production screens use MoversDesignTheme with the approved defaults. The gallery nests its own local theme; preview adjustments never change application preferences. Android dynamic colours no longer replace the shared brand palette.
- Buttons, fields, menus, dialogs, sheets, list rows/cards and notices are shared components. A deliberate Material bridge maps retained text, icon, scaffold, progress and snackbar-host infrastructure to Movers tokens; it does not use Material control styling. Existing navigation, backend contracts and domain behaviour remain feature-owned.
- Android eligibility uses the installed application's debuggable flag; iOS uses Kotlin/Native's debug-binary flag. Release/TestFlight have no launcher or runtime unlock. The app sanitizes all restored guide keys before providers run and rejects release navigation attempts; the entry is independently guarded. Its serializer remains available for safe restoration.
- This is a visibility/access boundary, not a promise to strip all guide code/assets from release packages. Backend and personal configuration remain private as before.

## Components and sources

Edit palettes/type/spacing in [MoversDesignTheme](../../designsystem/src/commonMain/kotlin/com/jcotters/designsystem/MoversDesignTheme.kt), defaults in [MoversStyleSettings](../../designsystem/src/commonMain/kotlin/com/jcotters/designsystem/MoversStyleSettings.kt), and component wrappers alongside them. Gallery stories live in the style-guide feature. Fields support local state-based Unstyled input and controlled Foundation input for ViewModel-owned text without a second synchronized text copy. Wrappers retain labels, IME actions, paste/selection and focus. Controls retain at least 48 dp touch targets; content wraps rather than relying on fixed text heights.

The style combines references from [Sequel](https://www.getsequel.app/), [Linear](https://linear.app/features), [RikkaUI](https://github.com/rainxchzed/RikkaUi) and [shadcn-multiplatform](https://github.com/jershell/shadcn-multiplatform). Wrappers/icons/artwork are original; no component source or visual assets were copied from those projects. See [provenance and bundled notices](../../designsystem/SOURCES.md).

Selected **Compose Unstyled 2.10.2** modules supply button/field/menu/dialog/sheet mechanics. Exact APIs were checked against Maven source jars and [official documentation](https://composeunstyled.com/docs/getting-started/installation/). Inter **v4.1** uses three static weights with its OFL licence. Both licences are shared resources and confirmed in the debug APK. Fonts total 1,248,684 raw bytes / 636,480 compressed APK bytes, not a whole application size delta.

**Haze is deferred**, including its dependency. Add a small effects gallery later, measure on physical devices and account for native-player/older-Android fallbacks before production adoption. The approved style is now adopted in the app; Haze remains a separate later change.

## Layout rules

- Film rows follow text height with a minimum poster size; the poster fills that height. Keep row sizing in the shared component rather than adding fixed heights in feeds/search. Image children use match-parent sizing so remote image dimensions do not determine row height; see [Compose intrinsic measurements](https://developer.android.com/develop/ui/compose/layouts/intrinsic-measurements).
- Grid cards round only their posters. Use `imageCornerRadius` (8 dp by default) for poster/backdrop clipping; `cornerRadius` remains 16 dp for controls. Do not clip the title/year region or wrap the image in a second control-radius clip.
- Swipe foregrounds match the screen background, including gutters. Coloured surfaces belong to intentional panels/overlays rather than accidental padded row rectangles.
- Search uses one inline-hint control with a persistent accessible label. Keep 48 dp action targets; do not add vertical field padding around those targets. The gallery uses the production search component.
- Sheets own their outer padding once. Add-to-list rows reuse the labelled checkbox control with concise metadata. Quick membership and avatar-colour saves use guarded controls and error feedback without progress bars/spinners; initial screen loading and longer-running work remain separate.

## State ownership and validation

Keep reusable UI in `:designsystem`, domain-aware movie adapters in `:presentation`, and actions/derived state in feature ViewModels or focused state models. Components accept values, slots and callbacks, with no repositories or service lookup. List query restoration/filtering, import summaries and export serialization belong to feature models; native file dialogs, focus, scrolling and popup visibility belong to Compose. Dialogs have one scrolling owner; sheet bodies with a LazyColumn must stay bounded. Menus limit their scroll viewport to the safe window height.

Use existing checks under the substantial-change build policy; add no new test cases during this phase. Latest evidence lives in the [handoff](../workflow/handoff.md). Physical Android/iPhone review should cover light/dark themes, large text, long titles, focus/IME, menu and sheet scrolling/dismissal, multi-list selection, swipe Undo and native file save/share. The missing iOS Swift entry remains a separate app-build prerequisite.

# Current handoff

Updated 11 November 2026. Source checkpoint `44f511c`; approved development-only style guide implemented in three code commits plus verification/documentation. See the [guide and ownership rules](../design/style-guide.md). Production UI redesign and Haze remain deferred until the guide is reviewed. [Earlier feature/release evidence](history/2026-11-11-before-style-guide.md) is historical, not current runtime acceptance.

## Implemented

- **Access:** Profile → Developer tools → Style guide. Android checks the installed application's debuggable flag; iOS checks Kotlin/Native's debug-binary flag. No runtime unlock setting. The app owner removes restored guide keys from every release stack before entry providers run and rejects release navigation attempts; the entry is guarded as well. Serializer stays registered for safe restoration.
- **Ownership:** `:designsystem` holds experimental Foundation/Unstyled components and Inter assets; `:features:styleguide` + contract hold typed navigation and local gallery state. Existing conventions apply; no backend/schema changes. Production theme, bars and controls stay as before except the development launcher.
- **Gallery:** Basics, Controls, Overlays and Films. One curated style with local System/Light/Dark, 8/12/16 dp corners, spacing and Reset. Entry ViewModel owns settings; per-section saved state retains inputs/scroll. Original synthetic posters, titles and participants; no repository calls or real list actions. Demo memberships belong to each film. Popup payloads remain stable through dismissal animations.
- Working button/field/selection, menu/dialog/sheet/feedback, poster/row/rating/avatars/list/activity examples. Sources/notices live in [design-system provenance](../../designsystem/SOURCES.md); Inter v4.1 and selected MIT Unstyled 2.10.2 dependencies. No Rikka/shadcn source copied and no Haze dependency. New gallery controls are experimental, not the new production default.

## Verification and limits

- Final focused command: `:shared:testAndroidHostTest :app:assembleDebug :shared:compileKotlinIosArm64`. Passed in 19 s; 41 executed / 451 up-to-date. Current shared reports: **19 tests / 5 suites**, no failures/errors/skips. Existing key-roundtrip assertion includes StyleGuideKey; no new cases. Private log: `/private/tmp/movers-styleguide-verified.log`.
- Shared physical-iOS arm64 source compilation passed; no Xcode app archive/link or native runtime acceptance claimed. The native check caught a JVM-only star-drawing constant, corrected to common Kotlin PI before the final pass.
- Debug APK contains all three Inter fonts and both licence notices. Fonts contribute 636,480 compressed bytes (1,248,684 raw); not a whole APK/IPA size delta. Static token contrast, input outlines, minimum touch targets, focus/press feedback and source/dependency boundaries reviewed. Docs/diff checks passed. No device/simulator interaction, connected tests, backend deployment or upload.
- Existing iOS app setup issue persists: `iosApp/MoversApp.swift` is absent while Xcode/XcodeGen reference it. Its earlier deletion was preserved. Resolve that separate prerequisite before an app archive or new physical-iPhone build.

## Next

- User reviews the gallery on physical Android/iPhone: Debug-only access, release exclusion/restoration, theme isolation, large text, focus/IME, popup dismissal/dragging, reset/section retention, and per-film demo membership. Host checks do not cover those new journeys or prove visual quality. Maintain current no-routine-build/no-new-cases/no-emulator rules.
- Refine this one style before planning production adoption. Later add a small Haze effects lab; nothing installed yet. Private ignored Todo remains authoritative. Home filters, four Profile favourites and watched-status design remain separate.
- Prior physical checks remain pending: navigation/keyboard and short-list clipping, shared collaboration/attribution, native import/export, swipe Undo, first-use tour and trailer playback/lifecycle. Backend import deployment was user-confirmed; no new upload after 0.2.0 (3).

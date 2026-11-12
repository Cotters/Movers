# Current handoff

Updated 12 November 2026. Source checkpoint `b841cc7`, following design-system commit `16eb7a4`. The owner reviewed the guide, approved **16 dp corners / Comfortable spacing**, and authorized app adoption. [Earlier gallery evidence](history/2026-11-11-style-guide-before-app-adoption.md) is historical. See the [design-system guide](../design/style-guide.md).

## Implemented and ownership

- Android/iOS share the approved graphite/ivory/mint/teal palette and Inter typography. System light/dark mode is respected; Android dynamic-colour overrides and starter theme files are retired. Gallery adjustments stay local. Haze remains deferred, including its dependency.
- `:designsystem` owns stateless controls, slots/callbacks, film primitives and semantic colours. Buttons, fields, menus, dialogs, sheets, cards/list rows, avatars, fractional stars and notices are used by production screens and the gallery. `:presentation` adapts movie/participant data to those primitives. Dependencies are explicit in each consuming module, using existing conventions.
- A centralized Material bridge maps retained text/icon/scaffold/progress/snackbar-host infrastructure to Movers tokens. Controls use Foundation/Unstyled mechanics and Movers styling. Snackbar timing and Undo results still use the existing host. Menus have a safe-window-bounded scrolling viewport; dialogs own their scrolling once; lazy sheet bodies remain bounded.
- Activity **added/imported is green; removed is red**, with italic film titles. The owner's correction supersedes the old gallery colours. Persisted avatar colours stay unchanged, with one reusable initial renderer.
- Feature ViewModels/state models own list query/filtering, import summary/status/selection and export serialization. Watchlist queries use the existing SavedStateHandle pattern. Native file-picker/share calls, focus, scroll/gesture mechanics and transient popup visibility remain with Compose; no second repository path or service lookup in design components.
- Home/search, movie details, Watchlists/sharing/members/activity, import/export, onboarding and Profile use the new controls. Profile handles long names and scrolls at large text sizes. Backend contracts/schema, identity and feature actions are unchanged.
- Debug-only guide navigation and release/restoration guards remain intact. No new test cases or third-party library versions; no platform targets, backend deployment, version/signing changes or uploads. Private ignored Todo updated; Human feedback preserved.

## Verification and limits

- Final command: `testAndroidHostTest :app:assembleDebug :shared:compileKotlinIosArm64`, JDK 17. **79 existing tests / 21 suites**, zero failures/errors/skips. Build passed in 32 s; 97 executed / 431 up-to-date. Private log: `/private/tmp/movers-ui-refresh-final.log`.
- Existing host journeys caught a nested tour scroll container and an unbounded scrolling dropdown. Corrected at their layout owners; the complete suite passed afterward. Assertions were not weakened and no cases were added.
- Android debug APK contains three Inter fonts and both licence notices. Source/dependency/diff and documentation-link checks passed. Shared physical-iOS arm64 source compilation passed; existing native file-action BetaInterop opt-in warnings remain visible. No native app link/archive/runtime or visual acceptance is claimed.
- The existing missing `iosApp/MoversApp.swift`, still referenced by Xcode/XcodeGen, remains a separate app-build prerequisite. Its earlier deletion was preserved. No emulator/simulator/device use or connected tests.

## Next

- Owner reviews the refreshed app on physical Android/iPhone: light/dark and large text, long titles/names, bottom bar/keyboard behaviour, menu scrolling/dismissal, forms, Add to list, swipe Undo and native file save/share. The earlier positive gallery review is not acceptance of these newly styled production flows.
- Haze effects lab, Home filters, four Profile favourites and watched-status design remain separate Todo work. Expanded automated coverage remains post-feature. Retain no-routine-build/no-new-tests/no-device rules; models own feature logic, Compose owns transient UI mechanics.

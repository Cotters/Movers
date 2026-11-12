# UI design and Compose review

Read before changing shared layout or visual hierarchy. The [style guide](style-guide.md) owns approved Movers styling; these rules describe how we make and implement layout decisions.

## Agree the layout

- Describe the hierarchy and relationships before editing: primary content, supporting metadata, actions and attribution. Choose placement for that meaning, not because an existing Row has room.
- **Ask about ambiguous placements.** State the exact edge/alignment and relevant alternatives. Push back when instructions conflict, reduce readability or create awkward spacing; explain the tradeoff and wait for the missing decision. Clear instructions need no repeated approval.
- For a meaningful redesign, show a small sketch or gallery example before spreading it across screens. An approved palette/component gallery does not approve every later screen composition.
- Group related information; use spacing to distinguish roles. Film details stay together; contributor attribution has a separate footer. The agreed watchlist footer is bottom-right in both rows and grid cards, off the artwork.

## Implement the agreed design

- Keep reusable geometry, spacing and interaction in `:designsystem`; domain-aware adapters in `:presentation`; feature actions and derived data in ViewModels/state models. Focus, measurement and gestures stay with Compose.
- Use semantic slots such as `footerContent`, with placement owned by the component. Prefer direct values/events and simple composable lambdas. Avoid callback factories, unrelated boolean switches and extra abstraction layers that make the rendered structure hard to follow.
- Use standard Row/Column/Box constraints first. Let text wrap within agreed line limits and grow naturally; no fixed text heights or coordinate/state feedback loops to patch alignment. Intrinsics need a specific sibling-sizing purpose; avoid nested intrinsic chains.
- Give padding, surfaces and clipping one clear owner. Round artwork independently of controls. Preserve image aspect ratios; do not clip metadata or move attribution onto artwork to save space.
- Separate visible icon/avatar size from its hit area. Movers retains at least 48 dp interaction targets, meaningful action labels, focus/press feedback and readable large text. Do not squeeze avatars into padding intended for smaller icons or announce the initial twice.
- Keep functions focused on a visual responsibility. Extract meaningful repeated roles; do not split every layout node into a helper or build new generic component frameworks for one placement.

## Review honestly

Use the [required code review routine](../workflow/code-review.md) after code changes and review fixes. Read the complete affected component and its callers, not only added lines; trace padding/constraints to the visible child and remove obsolete layout paths. Check short/long titles, absent metadata, attribution present/absent, edit actions, narrow widths, large text, light/dark and RTL implications. Keep representative gallery examples using production components and synthetic data.

Follow the existing no-routine-build/no-new-tests policy. Static checks and compilation cannot establish visual quality. People verify on physical devices; agents never use emulators/simulators. Report the exact remaining visual check rather than claiming the layout is verified.

## Sources and skills

Reviewed 12 November 2026. These inform implementation; placement and approved token choices remain project decisions.

- [NN/g: proximity](https://www.nngroup.com/articles/gestalt-proximity/) (2 August 2020): visual grouping and responsive relationships.
- [AndroidX component API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md) (maintained source; dated 19 July 2023): focused components, modifier conventions and composable slots.
- [Compose intrinsic measurement](https://developer.android.com/develop/ui/compose/layouts/intrinsic-measurements) and [state ownership](https://developer.android.com/develop/ui/compose/state-hoisting) (living docs): constraints and state responsibilities.
- [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults) and [CMP accessibility](https://kotlinlang.org/docs/multiplatform/compose-accessibility.html) (living docs): touch areas, semantic roles and labels across platforms.

Use the installed `jetpack-compose-performance` skill and its layout reference when reviewing measurement/performance concerns; it does not decide visual placement. Use inset/native skills only for their relevant problem. Read matched references and check consumed library versions; do not apply a skill checklist wholesale or install extra skills without an identified gap and authorization.

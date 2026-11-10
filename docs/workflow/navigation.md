# Navigation ownership and acceptance

The shared Compose app owns retained Home, Watchlists, and Profile tab stacks. Feature contract modules own serializable NavKeys; features provide their entries. Koin creates ViewModels using each Nav 3 entry's ViewModelStore. The composition module registers all key serializers explicitly for Android and iOS.

Home is the infinite movie feed. Watchlists owns custom lists, list details/management, and the multi-list quick-add sheet. A separate Search feature supplies global and list-targeted search. Profile shows the confirmed device name/avatar, and name editing. Saved films belong to Watchlists. Name entry precedes the main shell on first launch; the feature tour remains deferred. There is no Auth destination, password entry or logout. Sharing uses anonymous backend sessions internally; see the [feature design](../features/shared-watchlists.md) for invitations, Members and Activity.

- Switching tabs preserves their histories and root scroll state.
- Reselecting the active tab pops it to its root.
- Back pops feature destinations; Back at a non-Home root selects Home's retained history. Android Back at Home root exits through the platform.
- Detail entries for the same movie in different stacks own different ViewModel stores. Repeated taps on the current movie do not duplicate its entry.
- Saved navigation uses explicit polymorphic key serialization, with no Android reflection requirement. The Android host ignores pre-migration saved activity state once, then preserves the new state normally.
- The bottom bar stays visible on the main screens, search, and details. The quick-add modal overlays the retained source and stays open while check/uncheck saves membership immediately; Close returns to the source. Startup loading/storage failure is handled before the navigation shell; no onboarding screen is implemented.
- The shared detail header has an accessible Back button wired by its feature entry provider to the Navigator. Host UI tests cover this action and Android system Back; the shared native smoke flow uses the visible button on both platforms. iOS edge-gesture acceptance remains pending.

List/search/picker keys live in feature contracts and are registered by `:shared`. Each entry owns its ViewModel; the search query uses SavedStateHandle, while picker checkboxes come from persisted membership. The shared Nav 3 overlay scene strategy presents the bottom sheet on both platforms. Returning to a source retains its query, paging, scroll, and navigation state. A target list is context, not authorization.

## Expanded movie artwork

Tapping an available detail poster or backdrop opens `MovieImageKey` in the current tab. Close/platform Back returns to the retained detail; the bottom bar stays visible. The entry owns a Koin ViewModel observing the cached movie, without another detail refresh. Keys persist only movie/type/source-entry identity; `:shared` explicitly registers their serializer. Expansion identities include tab, source detail entry and poster/backdrop kind, separate from the catalogue/poster transition.

The viewer fits the complete image on a dark background, with safe-inset, 48 dp Close controls above the transition overlay. `sharedBounds` crossfades cropped/fitted content in the existing Compose transition hierarchy. The internal movie mapper supplies full-size URLs; provider-specific sizing stays outside the feature/UI and requires no Room migration. Coil keeps the thumbnail alias as placeholder/error fallback, and Retry recreates the image request composition. Inspection-mode previews avoid live loader initialization. No pinch/pan interaction is implemented.

Research reviewed 10 November 2026: [Compose shared transitions and limitations](https://developer.android.com/develop/ui/compose/animation/shared-elements), [asynchronous-image guidance](https://developer.android.com/develop/ui/compose/animation/shared-elements/common-use-cases), [Coil Compose sizing](https://coil-kt.github.io/coil/compose/), [TMDB image URLs](https://developer.themoviedb.org/docs/image-basics), and [shared Nav3](https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html). Cross-checked transition/metadata/Koin APIs against installed sources. The Android CLI doc lookup could not acquire its sandboxed bundle lock, so official web documentation supplied the research. Build/device checks remain subject to the current workflow policy.

## Physical-device acceptance

Run these on your own Android device and iPhone, using configured movie API access:

1. Scroll Home, open a detail, switch to Profile, confirm name/avatar, and switch back to the retained detail. Confirm each tab retains its own detail/history and Home scroll position.
2. Tap the active tab again. Confirm it returns to its root without losing the other tabs' history; repeated taps at the root do not duplicate entries.
3. Pop details through the on-screen/platform Back behavior and iOS navigation gesture. Confirm the selected tab and Home-return policy remain coherent.
4. Confirm movie details offer Add to list without a bookmark action. Profile shows the confirmed name/avatar and name editing, with no bookmark shelf. Edit the name and relaunch to confirm it persists.
5. Check status/navigation-bar and safe-area insets, landscape, large text, and light/dark mode. The bar must stay visible on details, the image viewer and Profile. Tap poster/backdrop, verify expansion and an uncropped larger image, close/back to the same detail/scroll, and check each tab retains its own viewer. Repeat in landscape and offline: cached artwork remains usable, failed full-size loading offers Retry and Close stays tappable. Check missing-image films expose no expand action.
6. Create two lists (including equal names), add a catalogue film to both, open its details, and verify memberships are checked and editable. Uncheck/recheck to remove/add immediately, then close with no confirmation. Create another list inside the picker; it automatically receives the movie and persists when the picker closes.
7. In a list, tap +, search, add two films consecutively, and confirm search stays open with red Remove actions. Remove an accidentally added result, verify it returns to Add, then re-add without leaving search. Open a result detail, use quick-add, then return and check query/scroll. Rename/remove/delete and relaunch; verify list membership and movie details persist. Repeat cached list access offline and search failure/retry.
8. On Android, recreate the activity and test process restoration with a detail open in another tab. Confirm the selected stack restores without stale Auth keys or duplicate ViewModels.

The compiled `MoversNavigationShellTest` in `:shared` checks retained per-entry stores and saved-state restoration. Agents may compile it only under the requested/necessary build policy. Record physical-device observations, device/OS versions, and sanitized screenshots/logs before marking acceptance complete. No runtime result has been inferred from compilation or host policy tests.

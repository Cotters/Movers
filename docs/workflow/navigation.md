# Navigation ownership and acceptance

The shared Compose app owns retained Home, Watchlists, and Profile tab stacks. Feature contract modules own serializable NavKeys; features provide their entries. Koin creates ViewModels using each Nav 3 entry's ViewModelStore. The composition module registers all key serializers explicitly for Android and iOS.

Home is the infinite movie feed. Watchlists is a coming-soon placeholder. Profile shows the device name (temporarily Josh) and persisted bookmarks. There is no Auth destination, password entry, logout, or expiring session. Name entry and the first-launch feature introduction are deferred to Onboarding.

- Switching tabs preserves their histories and root scroll state.
- Reselecting the active tab pops it to its root.
- Back pops details; Back at a non-Home root selects Home's retained history. Android Back at Home root exits through the platform.
- Detail entries for the same movie in different stacks own different ViewModel stores. Repeated taps on the current movie do not duplicate its entry.
- Saved navigation uses explicit polymorphic key serialization, with no Android reflection requirement. The Android host ignores pre-migration saved activity state once, then preserves the new state normally.
- The bottom bar stays visible on the main screens and details. Startup loading/storage failure is handled before the navigation shell; no onboarding screen is implemented.
- The shared detail header has an accessible Back button wired by its feature entry provider to the Navigator. Host UI tests cover this action and Android system Back; the shared native smoke flow uses the visible button on both platforms. iOS edge-gesture acceptance remains pending.

## Physical-device acceptance

Run these on your own Android device and iPhone, using configured movie API access:

1. Scroll Home, open a detail, switch to Profile, open a bookmarked movie, and switch back. Confirm each tab retains its own detail/history and Home scroll position.
2. Tap the active tab again. Confirm it returns to its root without losing the other tabs' history; repeated taps at the root do not duplicate entries.
3. Pop details through the on-screen/platform Back behavior and iOS navigation gesture. Confirm the selected tab and Home-return policy remain coherent.
4. Bookmark/unbookmark a movie. Confirm Profile changes through persisted observation, shows Josh, and offers no account controls. Relaunch and confirm bookmarks/name persist.
5. Check status/navigation-bar and safe-area insets, landscape, large text, and light/dark mode. The bar must stay visible on details and Profile.
6. On Android, recreate the activity and test process restoration with a detail open in another tab. Confirm the selected stack restores without stale Auth keys or duplicate ViewModels.

The compiled `MoversNavigationShellTest` in `:shared` checks retained per-entry stores and saved-state restoration. Agents only compile it. Record physical-device observations, device/OS versions, and sanitized screenshots/logs before marking acceptance complete. No runtime result has been inferred from compilation or host policy tests.

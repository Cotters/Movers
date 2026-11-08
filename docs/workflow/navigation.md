# Navigation ownership and acceptance

Implemented against the agreed 8 November 2026 plan. Runtime/UI verification remains pending on the user's physical device.

## Boundaries

- Feature `contract` modules own serializable NavKeys. Existing feature modules implement their screens and contribute entry providers through Hilt. `:features:watchlists` currently contains only session-aware placeholder UI.
- `:navigation:contract` exports `Navigator` and `EntryProviderInstaller`. Installers receive navigation callbacks and a stack identifier; they do not own app navigation state or retain screen ViewModels.
- The app owns `NavigationState`, `AppNavigator`, logical tab selection, and the Material bottom bar. The logical Profile tab hosts either `AuthKey` or `ProfileKey`, so Auth remains an independent entry scope without becoming a fourth tab.
- `:auth:contract` also exports `IUserRepository`, `UserSession`, and `MoverUser`; their Kotlin packages are unchanged. Profile and Details depend on that contract rather than the Auth implementation.

The app uses Nav 3 **1.2.1**, Lifecycle **2.11.0**, and AndroidX Hilt ViewModel Compose **1.4.0**, with the existing Gradle scripts/version catalog. Convention plugins remain a separate Todo item.

## State and lifetimes

Each tab has a saveable back stack, saveable UI-state holder, and ViewModel-store decorator. Decorators remain composed for inactive tabs. Screen ViewModels are created inside their entries; the AppViewModel alone uses the activity scope. Popping an entry releases its store, while changing tabs retains it.

Tab reselection pops that tab to its root without resetting root scroll position. Back first pops the current tab; at a non-Home root it selects Home's retained stack. Home root delegates Back to the activity/system exit behavior.

Login replaces Auth with Profile without resetting Home or selecting a different active tab. Passwords are cleared on successful login/signup and never enter NavKeys or saved navigation state. Credential Manager runs from the active Auth UI at RESUMED lifecycle state, once per entry, and supplies plain results to its ViewModel.

Restored state records its session owner. Private content is hidden until owner/root validation succeeds. Logout, expiry, or account replacement resets stacks to Home and changes the navigation generation, releasing every previous entry store. Login preserves the existing generation; keeping the decorators mounted during reconciliation preserves Home's state.

Details keys contain a movie ID and stable per-entry instance ID. Opening the same movie from Home and Profile creates independent stores. Poster transition keys include the originating stack; the existing image-cache keys remain unchanged.

The app shell owns bottom-bar/keyboard padding and consumes it before screen content. Existing screen scaffolds handle their remaining insets. The shared offline notice sits inside the content area above the bar/keyboard.

## Physical-device acceptance

Run the debug build on your physical device and report device model, Android version, failed steps, and screenshots or sanitized logs for any failure:

1. **Guest flow:** launch Home, open a movie, and confirm all three tabs remain visible. Watchlists shows “Auth Required.” Profile shows login/signup with the bar visible; both forms remain usable with the keyboard and large fonts.
2. **State and Back:** scroll Home, switch tabs, and return; the prior screen/scroll position returns. Reselect Home or Profile from movie details; it returns to that tab's root. Back from another tab's root selects Home's retained stack; Back at Home root exits. Check gesture Back and three-button navigation if available.
3. **Authentication:** sign in or sign up through Profile. Normal Profile appears in place, Watchlists changes to its coming-soon message, and Home retains its prior state. Logout returns Home; reopening Profile shows Auth without previous passwords or another account's content.
4. **Bookmarks and transitions:** bookmark/unbookmark and confirm existing Profile behavior. Open the same movie from Home and Profile, switch tabs, and verify correct content, Back destinations, poster transitions, and visible bottom navigation.
5. **Restoration:** rotate on details and verify the selected tab/movie. Background the app, kill its background process on the physical device with `am kill`, then reopen through Recents; verify tab/detail restoration. Repeat after session expiry when possible; protected content must not flash before returning Home.
6. **Connectivity and insets:** disconnect/reconnect while using each tab, including Profile with its keyboard open. Confirm the offline notice, controls, bottom bar, and system navigation do not overlap and recover correctly.

`MoversNavigationShellTest` additionally checks ViewModel lifetime and saved-state behavior using disposable probes. Run it with the Android test runner on your physical device. Its compilation alone does not satisfy these checks.

## References

Implementation follows Google's [modularization guidance](https://developer.android.com/guide/navigation/navigation-3/modularize), [multi-stack recipe](https://developer.android.com/guide/navigation/navigation-3/recipes/multiple-backstacks), [state/ViewModel guide](https://developer.android.com/guide/navigation/navigation-3/save-state), and [shared-element integration](https://developer.android.com/develop/ui/compose/animation/shared-elements/navigation). No emulator or simulator is part of this workflow.

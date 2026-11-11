# First-use feature tour

Implemented 11 November 2026. After the existing required name confirmation, show one short shared dialog: finding films, making watchlists and sharing with friends. **Get started**, **Skip**, Back and outside dismissal all finish the tour. It requests no permissions and introduces no extra setup steps.

Completion is installation-local: Android's existing SharedPreferences pattern and iOS UserDefaults, behind one shared preference interface. The flag is independent of the profile and database schema. The iOS app resource manifest declares app-owned UserDefaults access (CA92.1), with the resource present in both the checked-in project and XcodeGen specification. Existing installations without the flag see the tour once, too. Recreating the UI retains dismissal; restarting reads persisted completion. The navigation shell remains composed underneath, preserving tab histories and invitation routing.

The text scrolls when space or large font settings require it. The panel uses existing Compose components and adds no dependency. Name entry, identity, list writes and native import help are unchanged.

Research checked 11 November 2026: [Apple onboarding guidance](https://developer.apple.com/design/human-interface-guidelines/onboarding), [Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog), [SharedPreferences](https://developer.android.com/training/data-storage/shared-preferences) and [UserDefaults](https://developer.apple.com/documentation/foundation/userdefaults) and [required-reason API declarations](https://developer.apple.com/documentation/bundleresources/describing-use-of-required-reason-api). Retaining the existing preference approach avoids an unrelated storage migration; Android recommends DataStore for new storage architectures.

Static/source/docs review only; no new tests or builds. Physical Android/iPhone acceptance: name first, Get started/Skip/dismiss, no repeat after relaunch, pending invitation navigation and large text. Persistence/runtime checks remain pending.

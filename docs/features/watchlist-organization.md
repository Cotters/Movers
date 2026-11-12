# Watchlist organization

Sort films from the sort control beside the film count. Release date, alphabetical and rating support ascending/descending order. Recently added preserves the existing default. Missing/invalid dates and ratings stay last in either direction; title/id ties are deterministic. Sorting applies after local filtering in list and grid layouts.

Preferences are installation-local, independent of shared-list contents and activity. A feature-owned storage interface has Android/iOS adapters using the app's existing native-preferences pattern. Common policy serializes updates, persists before publishing new state and propagates cancellation. Feature ViewModels own selection/filtering; components render values and events. No database/backend migration or new library is needed.

The current authorized follow-ups are overview swipe-to-pin (five maximum) with clearer separators, then film-search suggestions beneath local list matches. Each Todo gets a separate commit. Verification uses the existing suite after integration; new cases and device interaction remain deferred under project policy.

References: [Compose gestures](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-composables), [native Android preferences](https://developer.android.com/training/data-storage/shared-preferences), [UserDefaults](https://developer.apple.com/documentation/foundation/userdefaults) and [coroutine Mutex](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.sync/-mutex/). Android's docs prefer DataStore for new general-purpose settings; this small feature follows the current project adapter pattern without a storage-library migration.

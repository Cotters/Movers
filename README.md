# [Movers](https://avatar.fandom.com/wiki/Mover)
An Android and iOS movie app using [TheMovieDB](https://www.themoviedb.org/), Kotlin Multiplatform, and shared Compose UI.

## Shared lists

Build a watchlist locally for yourself or publish to a database and collaborate with friends.

Import films from CSV/text or pasted titles, review anything not found, and export any list as CSV, text or JSON. See [import/export](docs/features/list-import-export.md) for supported formats and the required shared-import backend update.

Configure your own instance using [Supabase setup](supabase/README.md); the [design](docs/features/shared-watchlists.md) explains permissions, offline behavior, device identity and replaceable adapters.

## Technologies
- Kotlin Multiplatform and Compose Multiplatform for shared logic and UI
- Navigation 3 with feature-owned NavKeys and retained bottom-navigation stacks
- Koin for dependency injection and entry-scoped ViewModels
- Coil for Images
- Ktor with OkHttp on Android and Darwin on iOS
- Room with bundled SQLite for the device profile, watchlists, sharing cache, and movie cache
- MockK for Mocking

## Preview

Home has a feed showing popular movies and a movie detail view showing more info with expandable images.
Watchlists contains user created lists of films. These can be personal on-device, or shared with friends and hosted on a database/backend.
Simple add feature in every view to expand your watchlsits.

https://github.com/user-attachments/assets/0abc34e9-c8ef-48e5-aee7-b40896a8541a




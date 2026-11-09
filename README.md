# [Movers](https://avatar.fandom.com/wiki/Mover)
An Android and iOS movie app using [TheMovieDB](https://www.themoviedb.org/), Kotlin Multiplatform, and shared Compose UI.

## Development workflow

Start with [Human.md](docs/workflow/Human.md) for the workflow, its purpose, and a place to suggest improvements. [Development guidance](docs/workflow/development.md), [verification setup and commands](docs/workflow/testing.md), and [the current handoff](docs/workflow/handoff.md) preserve the details between contributors and AI sessions.

After completing the verification prerequisites, run `./scripts/verify.sh` for host tests, lint, and an Android debug build. On macOS, `./scripts/verify-ios.sh` builds the unsigned iOS app with the physical-device SDK. See [multiplatform setup](docs/workflow/multiplatform.md) for configuration and ownership. Physical-device acceptance remains user-owned.

Agents run host checks and builds only. People verify on physical Android/iOS devices; agents never start or interact with emulators/simulators or run connected tests. Retained [native smoke flows](docs/workflow/testing.md#shared-native-smoke-flow) remain available for user-owned validation. Host Compose UI regressions run through `verify.sh`.

## Shared lists

Publish a personal list, invite friends, and collaborate with contributor avatars and film activity. Local lists work without a backend. Configure your own instance using [Supabase setup](supabase/README.md); the [design](docs/features/shared-watchlists.md) explains permissions, offline behavior, device identity and replaceable adapters.

## Technologies
- Kotlin Multiplatform and Compose Multiplatform for shared logic and UI
- Navigation 3 with feature-owned NavKeys and retained bottom-navigation stacks
- Koin for dependency injection and entry-scoped ViewModels
- Coil for Images
- Ktor with OkHttp on Android and Darwin on iOS
- Room with bundled SQLite for the device profile, bookmarks, and movie cache
- MockK for Mocking

## Preview

While this is a work in progress, here's a preview of the app.

Historical Android preview: bookmarking and movie-poster transitions. Its sign-in/logout UI has since been replaced by a device profile; users now choose a name on first launch. A feature introduction remains deferred.

https://github.com/user-attachments/assets/96d1be5d-352e-4a44-8161-f212dc407207


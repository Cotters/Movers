# [Movers](https://avatar.fandom.com/wiki/Mover)
An Android and iOS movie app using [TheMovieDB](https://www.themoviedb.org/), Kotlin Multiplatform, and shared Compose UI.

## Development workflow

Start with [Human.md](docs/workflow/Human.md) for the workflow, its purpose, and a place to suggest improvements. [Development guidance](docs/workflow/development.md), [verification setup and commands](docs/workflow/testing.md), and [the current handoff](docs/workflow/handoff.md) preserve the details between contributors and AI sessions.

After completing the verification prerequisites, run `./scripts/verify.sh` for host tests, lint, and an Android debug build. On macOS, `./scripts/verify-ios.sh` builds the unsigned iOS app with the physical-device SDK. See [multiplatform setup](docs/workflow/multiplatform.md) for configuration and ownership. Physical-device acceptance remains user-owned.

Run the [shared native smoke flow](docs/workflow/testing.md#shared-native-smoke-flow) on already-running Android and iOS targets after each change set. These automated runs are authorized; manual device interaction and starting/resetting targets require a separate request. Host Compose UI regressions run without devices through `verify.sh`.

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

Historical Android preview: bookmarking and movie-poster transitions. Its sign-in/logout UI has since been replaced by a device profile; the temporary name is Josh. First-launch name entry and a feature introduction are deferred.

https://github.com/user-attachments/assets/96d1be5d-352e-4a44-8161-f212dc407207


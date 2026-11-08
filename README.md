# [Movers](https://avatar.fandom.com/wiki/Mover)
Using [TheMovieDB](https://www.themoviedb.org/) API to present movies in an Android app built with Jetpack Compose!

## Development workflow

Start with [Human.md](docs/workflow/Human.md) for the workflow, its purpose, and a place to suggest improvements. [Development guidance](docs/workflow/development.md), [verification setup and commands](docs/workflow/testing.md), and [the current handoff](docs/workflow/handoff.md) preserve the details between contributors and AI sessions.

After completing the verification prerequisites, run `./scripts/verify.sh` for host tests, lint, and a debug build. Android runtime/UI verification is performed by the user on a physical device; agents never use emulators or simulators.

## Technologies
- Jetpack Compose for UI
- Navigation 3 with feature-owned NavKeys and retained bottom-navigation stacks
- Hilt for Dependency Injection
- Coil for Images
- OkHttp & Retrofit for Networking
- Room for Storage (Used for Auth, Profile with Bookmarks, and caching Movies locally)
- MockK for Mocking

## Preview

While this is a work in progress, here's a preview of the app.

Showing: Reactive Auth flow (Sign In & Logout), Bookmarking movies to Profile, Shared Element Transitions with Movie Poster.

https://github.com/user-attachments/assets/96d1be5d-352e-4a44-8161-f212dc407207


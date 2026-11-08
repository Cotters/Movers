# Development with AI

Reviewed: 8 November 2026. This guide describes the existing app and the workflow agreed for this repository. Code and build configuration remain the source of truth; update this guide when an agreed change makes it stale.

## Work cycle

1. Inspect Git state and the relevant code/tests. For substantial work, read the current handoff. Clarify behavior, success criteria, and consequential unknowns before implementation.
2. Propose a plan grounded in existing code. Identify affected boundaries, failure cases, verification, and any physical-device checks. Obtain agreement before feature implementation.
3. Implement focused, reviewable changes. After plan agreement, continue through the authorized implementation and host checks without repeated permission requests. Revisit the plan when product requirements or material scope change.
4. Review the diff for correctness, maintainability, accidental credentials, and unrelated edits. Run checks proportionate to the change; use the full verification script before handing back feature or tooling changes.
5. Explain what changed, what was verified, and what remains uncertain. Update the handoff after meaningful progress and the appropriate guide after an agreed workflow/architecture change. Record useful lessons with their reason and code evidence, rather than accumulating generic rules.

Treat suggestions in Human.md as proposals until agreed. Preserve user feedback, and record the outcome when a proposal is accepted or declined. No separate approval is needed to run ordinary authorized host checks or repair failures introduced by the change.

This is a portfolio/demo project intended for public reuse. For backend work, follow [public-source and portability guidance](open-source.md): repositories depend on a separate vendor-neutral database API, and Supabase-specific adapters implement it. The private ordered queue is `Notes/Todo.md` when present; its first items are Navigation, then Shared Lists. Keep durable requirements in this tracked folder so a clone does not depend on private notes.

## Existing architecture

| Area | Current responsibility |
| --- | --- |
| `:app` | Application composition, Hilt entry points, theme, typed navigation graphs, session-driven navigation, and the app-wide offline notice. |
| `:features:catalogue`, `:features:details` | Compose feature screens and ViewModels; Details also owns bookmark-related use cases. |
| `:auth`, `:profile` | Existing feature modules containing UI, domain contracts/use cases, data implementations, and DI wiring. |
| `:movie:contract` | Movie domain models and repository interfaces consumed by features. |
| `:movie:internal` | Movie API, repository implementations, mapping, paging mediator, refresh/cache policy, and DI providers. |
| `:database` | Room entities, DAOs, exported schemas, explicit migrations, and database providers. Current database version: 6. |
| `:presentation`, `:data:network` | Shared presentation resources/components and device-network observation respectively. |
| `:tests` | Android test module targeting the app; its presence does not establish executed test coverage. |

The existing modules are not uniformly split into contract/internal pairs. Follow the relevant neighboring code and choose boundaries for a concrete need; do not duplicate the movie split for every feature or migrate the app to a new state framework.

Representative flows, relative to the repository root:

- **Bookmark write:** [BookmarkMovieUseCase](../../features/details/src/main/java/com/jcotters/details/domain/BookmarkMovieUseCase.kt) resolves the current user through `IUserRepository`; `IBookmarksRepository` delegates persistence to [BookmarksRepository](../../movie/internal/src/main/java/com/jcotters/internal/detail/data/BookmarksRepository.kt) and the Room DAO. Keep data access out of composables.
- **Bookmark observation:** [ObserveUserBookmarksUseCase](../../profile/src/main/java/com/jcotters/profile/domain/ObserveUserBookmarksUseCase.kt) uses `flatMapLatest` on the session to switch subscriptions and clear prior-user content. New account-scoped observations must not retain another user's data.
- **Screen state:** [MovieDetailViewModel](../../features/details/src/main/java/com/jcotters/details/ui/MovieDetailViewModel.kt) handles view events, exposes state, observes saved movies, and coordinates refresh/bookmark work. Screens render state and send events; [MovieDetailScreen](../../features/details/src/main/java/com/jcotters/details/ui/MovieDetailScreen.kt) includes previews for loading, partial content, and failure.
- **Navigation:** [NavigationRoutes](../../app/src/main/java/com/jcotters/movers/ui/NavigationRoutes.kt) contains serializable routes. [MoversApp](../../app/src/main/java/com/jcotters/movers/ui/MoversApp.kt) owns session-driven navigation; avoid adding competing navigation on authentication success.
- **Persistence/cache:** [MovieDetailsRepository](../../movie/internal/src/main/java/com/jcotters/internal/detail/data/MovieDetailsRepository.kt) separates Room observation from refresh, preserves cached data on refresh failure, and injects a clock. Catalogue paging uses a mediator and its own freshness policy. Do not silently replace these policies when adding a feature.

## Lessons to preserve

- **Persisted data owns saved state.** Observe Room for bookmarks and saved movies instead of maintaining independent UI copies. Represent loading, empty, partial success, and recoverable failure where the outcome matters.
- **Cancellation is not a normal failure.** Propagate `CancellationException`; avoid catching fatal errors as recoverable failures. Inject clocks/dispatchers where timing or scheduling needs control.
- **Concurrency rules need context.** Use atomic `MutableStateFlow.update` for changes derived from previous state. Direct assignment of a complete independent value is not inherently wrong. Guard overlapping writes and stale responses where required by observable behavior.
- **Persistence is an installed-data contract.** Preserve accounts, bookmarks, and their relationships through explicit migrations and schema exports. Historical version-2 databases had multiple schema variants. Do not introduce destructive recreation to bypass migration work.
- **Define the success boundary.** Account creation and session creation are separate operations; a failure after a committed write can require recovery rather than repeating the entire operation.
- **Keep lifecycle ownership clear.** Use lifecycle-aware collection in the UI, structured coroutine scopes, and an appropriate UI owner for Activity-dependent APIs. Rotation, backgrounding, and process death have different consequences.
- **Match the current dependency versions.** Use the version catalog and wrapper. Built-in Kotlin/new-DSL migration is existing follow-up work, not an incidental feature prerequisite. Do not copy Room 3 or Navigation 3 APIs into the current stack without an agreed migration.
- **Local accounts are local.** Existing authentication checks Room users and persisted local sessions; it does not establish backend identity or remote authorization. Never upload existing password hashes as a shortcut to remote authentication.

These lessons were checked against current code and curated from private study notes. Old descriptions of “remaining” work are not automatically current: detail persistence, freshness, and retry behavior have since changed. Recheck source before opening a task.

## Research behind the workflow

Research window: **8 May–8 November 2026**, reviewed 8 November. Official articles inform the approach; community comments illustrate experience and disagreement, not platform guarantees.

| Source | Useful conclusion for Movers |
| --- | --- |
| [Android Studio I/O Edition, Google, 19 May 2026](https://android-developers.googleblog.com/2026/05/whats-new-android-developer-tools.html?m=1) | Android CLI, skills, and official documentation can ground an agent in Android-specific tools and knowledge. Use applicable capabilities within the user's testing restrictions. |
| [Inside Android Skills, Google, 6 August 2026](https://android-developers.googleblog.com/2026/08/android-skills-philosophy.html?m=1) | Prefer focused skills that address a demonstrated gap. Do not load a large collection of generic guidance for every task; evaluate whether a skill improves results. |
| [Iterating Development Workflows with Codex, OpenAI, 3 August 2026](https://developers.openai.com/cookbook/examples/codex/iterating-development-workflows-with-codex) | Keep repository instructions, plans, and evidence durable. Its extra file conventions are optional; this app uses a small linked documentation set and a handoff. |
| [Rethinking skills and prompts, OpenAI, 11 September 2026](https://developers.openai.com/blog/rethinking-skills-and-prompts-for-gpt-6-astra) | Keep instructions concise, load supporting material when relevant, and make completion boundaries explicit. Apply the context-management lessons without making project policy depend on one model. |
| [Compose agent-rules discussion and comments, r/androiddev, May 2026](https://www.reddit.com/r/androiddev/comments/1tjfn3t/i_got_tired_of_ai_agents_breaking_my_compose_code/) | The thread proposes rules about state, lifecycles, and lists; comments correct the blanket ban on direct state assignment. Verify the rationale before adopting a rule or architecture migration. |
| [Android teaching/AI discussion and comments, r/androiddev, August 2026](https://www.reddit.com/r/androiddev/comments/1vr35pn/advice_for_my_android_students_about_using_ai/) | Contributors disagree about AI use but highlight the importance of understanding and reviewing generated work. This workflow keeps product decisions and review with people. |

For technical decisions, consult [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations), [coroutine testing](https://developer.android.com/kotlin/coroutines/test), and [Room migration guidance](https://developer.android.com/training/data-storage/room/migrating-db-versions) as relevant. Confirm examples match installed versions. The installed Android CLI's `android docs` command is preferred when available; official web documentation is the fallback if its access is blocked.

## Shared Watchlist boundary

Confirmed intent: the user and a friend can add to and review shared watchlists on separate devices. Supabase Free is the preferred candidate, not an implemented dependency or a provisioned service. At review time its [pricing](https://supabase.com/pricing) lists a 500 MB database and pausing after one week of inactivity; recheck terms before implementation.

Before feature work, agree on account integration, handling existing local users/bookmarks, membership/invites, permitted actions, offline behavior, synchronization/conflicts, and acceptance scenarios. Consult current Supabase documentation and the relevant skills. Remote access requires backend-enforced authorization; exposed tables need appropriate RLS policies, and a service-role/secret key must never enter the Android app. The [Kotlin client is community-maintained](https://supabase.com/docs/reference/kotlin/introduction); evaluate it against the existing networking stack during feature planning. No SDK, schema, API, or sync strategy is chosen by this workflow setup.

## First feature pass: Navigation (requested, not implemented)

- Migrate the current Nav 2 implementation to **Navigation 3** with serializable `NavKey`s, proper feature modularization, and explicitly scoped ViewModels. This migration is explicitly requested; keep unrelated toolchain upgrades separate.
- Provide **Home** (existing infinite movie feed), **Watchlists** (the future home for user-created lists), and **Profile**. First pass is navigation; list creation/sharing is later work. Preserve Profile behavior and existing bookmarks.
- Keep the bottom bar visible throughout main navigation, including movie details. Auth presents modally and hides the bar. Exact modal presentation and guest/return navigation need agreement in the feature plan.
- Use Google's [Navigation 3 skill](https://github.com/android/skills/tree/main/navigation/navigation-3), [modularization guidance](https://developer.android.com/guide/navigation/navigation-3/modularize), and [entry-state/ViewModel scoping guidance](https://developer.android.com/guide/navigation/navigation-3/save-state). Validate the multi-stack, modal Auth, and shared-poster transition design before replacing the current graph.

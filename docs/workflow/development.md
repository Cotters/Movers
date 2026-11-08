# Development with AI

Reviewed: 8 November 2026. This guide describes the existing app and the workflow agreed for this repository. Code and build configuration remain the source of truth; update this guide when an agreed change makes it stale.

## Work cycle

1. Inspect Git state and the relevant code/tests. For substantial work, read the current handoff. Clarify behavior, success criteria, and consequential unknowns before implementation.
2. Propose a plan grounded in existing code. Identify affected boundaries, failure cases, verification, and any physical-device checks. Obtain agreement before feature implementation.
3. Implement focused, reviewable changes. After plan agreement, continue through the authorized implementation and host checks without repeated permission requests. Revisit the plan when product requirements or material scope change.
4. Review the diff for correctness, maintainability, accidental credentials, and unrelated edits. Run checks proportionate to the change; use the full verification script before handing back feature or tooling changes.
5. Explain what changed, what was verified, and what remains uncertain. Update the handoff after meaningful progress and the appropriate guide after an agreed workflow/architecture change. Record useful lessons with their reason and code evidence, rather than accumulating generic rules.

Treat suggestions in Human.md as proposals until agreed. Preserve user feedback, and record the outcome when a proposal is accepted or declined. No separate approval is needed to run ordinary authorized host checks or repair failures introduced by the change.

This is a portfolio/demo project intended for public reuse. For backend work, follow [public-source and portability guidance](open-source.md): repositories depend on a separate vendor-neutral database API, and Supabase-specific adapters implement it. The private ordered queue is `Notes/Todo.md` when present; an explicit agreed task takes precedence. Keep durable requirements in this tracked folder so a clone does not depend on private notes.

## Existing architecture

Read [multiplatform ownership/setup](multiplatform.md) for the module map. Android's `:app` and the SwiftUI `iosApp` host both use `:shared`; feature screens, ViewModels, navigation, repositories, cache, and Room schema live in common Kotlin source sets. Preserve feature/contract and movie contract/internal boundaries. Koin definitions live with their feature/data owner and are aggregated at app composition. Platform source sets supply database paths, HTTP engines, connectivity, and Android dynamic colors.

Representative flows:

- [BookmarkMovieUseCase](../../features/details/src/commonMain/kotlin/com/jcotters/details/domain/BookmarkMovieUseCase.kt) calls the device-local bookmark repository, which persists through Room; there is no login requirement.
- [ProfileViewModel](../../profile/src/commonMain/kotlin/com/jcotters/profile/ui/ProfileViewModel.kt) observes the device profile and persisted bookmarks. New writes update the UI through observation, not independent saved-state copies.
- [MovieDetailViewModel](../../features/details/src/commonMain/kotlin/com/jcotters/details/ui/MovieDetailViewModel.kt) coordinates persisted details, refresh, retry, and bookmark events. [MovieDetailScreen](../../features/details/src/commonMain/kotlin/com/jcotters/details/ui/MovieDetailScreen.kt) renders state and sends events.
- [NavigationState](../../shared/src/commonMain/kotlin/com/jcotters/movers/ui/NavigationState.kt) owns retained tab stacks and explicit cross-platform key serialization. Features supply entries, and Nav 3 owns their ViewModel stores.
- [MovieDetailsRepository](../../movie/internal/src/commonMain/kotlin/com/jcotters/internal/detail/data/MovieDetailsRepository.kt) retains its 24-hour freshness policy and cancellation/concurrency rules; the catalogue mediator retains its separate six-hour policy.

## Lessons to preserve

- **Persisted data owns saved state.** Observe Room for bookmarks and saved movies instead of maintaining independent UI copies. Represent loading, empty, partial success, and recoverable failure where the outcome matters.
- **Cancellation is not a normal failure.** Propagate `CancellationException`; avoid catching fatal errors as recoverable failures. Inject clocks/dispatchers where timing or scheduling needs control.
- **Concurrency rules need context.** Use atomic `MutableStateFlow.update` for changes derived from previous state. Direct assignment of a complete independent value is not inherently wrong. Guard overlapping writes and stale responses where required by observable behavior.
- **Persistence is an installed-data contract.** Preserve version-7 device identity, bookmarks, and cached relationships through explicit migrations and schema exports. The user authorized resetting retired account databases (versions 1–6) for this migration only. Historical version-2 databases had multiple schema variants. Do not introduce destructive recreation to bypass migration work.
- **Define the success boundary.** A committed persistence operation and later UI/setup work are separate outcomes. Retrying initialization must not replace a stored installation ID or duplicate saved data.
- **Keep lifecycle ownership clear.** Use lifecycle-aware collection in the UI, structured coroutine scopes, and an appropriate UI owner for Activity-dependent APIs. Rotation, backgrounding, and process death have different consequences.
- **Match the current dependency versions.** Use the version catalog and wrapper. Android modules use AGP built-in Kotlin/new DSL and [composable build conventions](../../build-logic/README.md); keep one-off configuration local. Do not copy incompatible Room or Navigation APIs into the current stack without an agreed migration; this app uses Nav 3 version 1.2.1.
- **Local identity is local.** The current app has a local installation ID and temporary Josh display name, with no account/session system. Neither value establishes remote authorization. Backend identity and Shared Lists require their own agreed plan.

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

Before feature work, agree how device-local identity maps to backend authorization, membership/invites, permitted actions, offline behavior, synchronization/conflicts, and acceptance scenarios. The current app has no username/password accounts and does not provide cross-device identity or sync. Consult current Supabase documentation and the relevant skills. Remote access requires backend-enforced authorization; exposed tables need appropriate RLS policies, and a service-role/secret key must never enter the Android app. The [Kotlin client is community-maintained](https://supabase.com/docs/reference/kotlin/introduction); evaluate it against the existing networking stack during feature planning. No SDK, schema, API, or sync strategy is chosen by this workflow setup.

## Current navigation and migration

Home, Watchlists, and Profile share a retained Nav 3 shell on Android/iOS. Profile displays Josh and local bookmarks; Watchlists retains its coming-soon placeholder. Accounts, passwords, logout, and session expiry were explicitly removed. The migration may reset legacy local data once. First-launch name entry and a feature introduction are deferred to the private Onboarding Todo.

See [navigation policy/device acceptance](navigation.md) and [multiplatform setup](multiplatform.md). Kotlin 2.4.21 is the user-confirmed baseline; pursue working Android/iOS builds and address demonstrated compatibility problems. Do not re-open a speculative toolchain-upgrade project before feature work.

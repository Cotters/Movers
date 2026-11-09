# Development with AI

Reviewed 9 November 2026. These are current working agreements. The [retrospective and research](retrospective-2026-11-09.md) explain their rationale; the [handoff](handoff.md) holds active state. Read only what the task needs.

## Work cycle

1. Inspect Git status/HEAD, relevant code and existing checks. Locate the nearest working feature, actual consumed dependencies and platform boundaries before proposing changes.
2. For a new feature, agree outcome, consequential unknowns and scope. A clear follow-up can proceed directly. Keep a short requirement note in the handoff for substantial work: behavior, affected modules, exclusions, checks and unresolved assumptions. Do not create a new planning system for every task.
3. Implement one coherent slice at a time. Follow existing conventions, use focused checks during iteration and preserve unrelated edits. Product changes and toolchain migrations are separate tasks unless explicitly combined.
4. Review the resulting diff against requirements. During the feature phase, run existing tests and update intended expectations; add no new cases unless requested. Use [the verification decision table](testing.md#verification-by-change) and run the applicable final gates after changes settle.
5. Report outcome, evidence and limits; update current memory. A docs-only edit needs document validation, not an Android/iOS rebuild. Do not rerun successful checks to generate a newer timestamp when nothing relevant changed.

A useful substantial-task note is small:

```text
Outcome / changed behavior:
Affected modules and shared/platform ownership:
Out of scope / user decisions still needed:
Existing checks and native acceptance limits:
Current source checkpoint / next action:
```

People own product choices and physical-device acceptance. Continue authorized reversible implementation and host work without repeated approval questions. Do not bypass sandbox controls or mistake an environment approval for a new product decision.

## Debugging without repeated guessing

Keep a short working record, not another permanent log for every command:

| Observation | Hypothesis | Discriminating check | Result / next action |
| --- | --- | --- | --- |
| Exact failure and where it occurs | One explanation, labelled as unconfirmed | Smallest action that would support or reject it | Keep, revise or discard the explanation |

1. Wait for the active process to finish or identify its current state. Use a fresh log path per run; avoid concurrent Gradle/Xcode jobs against shared outputs. A filtered test run can overwrite a module's earlier JUnit XML, so do not mix their counts.
2. Read the complete failure chain and failing assertion. For a UI timeout, identify the expected screen/text and inspect its semantics/loading/error state. Inspect installed library source or exception definitions when needed. A timeout alone does not identify a deadlock, slow network or infinite recomposition.
3. Distinguish production behavior, fixture/harness behavior and environment/setup failure. An isolated pass followed by a suite failure suggests order/lifecycle/shared-state work to investigate; it does not prove an app defect.
4. Make one evidence-backed change, run the smallest relevant check, and inspect its result. After **two failed hypotheses**, stop speculative edits, collect missing diagnostics, and update the user with what is known and what the next check will distinguish. This is a project heuristic to prevent loops, not a research-proven threshold or an instruction to abandon the task.
5. Remove failed experiments and temporary diagnostics. Keep only the confirmed fix and useful failure reporting. Do not change product architecture merely to appease a host fixture, increase timeouts blindly, disable checks or present an unconfirmed cause as fact.

The confirmed Paging/Robolectric cleanup issue is recorded in [testing](testing.md#host-ui-regression-gate). Treat it as a specific lifecycle lesson, not a universal dispatcher workaround.

## Existing architecture and review

Android's `:app` and SwiftUI `iosApp` use `:shared`. Screens, state, navigation, repositories, cache and schema are common Kotlin. Platform source sets supply HTTP engines, paths, connectivity and native colors. Keep feature-owned Koin definitions and Nav 3 entry-owned ViewModels; see [module ownership](multiplatform.md).

Representative patterns:

- [MovieDetailViewModel](../../features/details/src/commonMain/kotlin/com/jcotters/details/ui/MovieDetailViewModel.kt): persisted details, refresh/retry, state and bookmark events.
- [ProfileViewModel](../../profile/src/commonMain/kotlin/com/jcotters/profile/ui/ProfileViewModel.kt): observable local profile and bookmarks.
- [NavigationState](../../shared/src/commonMain/kotlin/com/jcotters/movers/ui/NavigationState.kt): retained stacks and explicit key serialization.
- [WatchlistDatabase](../../movie/contract/src/commonMain/kotlin/com/jcotters/contract/watchlists/data/WatchlistDatabase.kt): provider-neutral storage contract behind repository policy.

Review non-obvious invariants: observable persisted membership rather than competing saved copies; cancellation propagation; scoped writes and pending-row guards; relationship-preserving Room migrations; resource packaging and cross-platform vector parsing; source/query/scroll retention. Use atomic StateFlow updates when deriving a new value from the old one; independent assignments are not categorically wrong. Preserve catalogue/detail freshness policies and independent bookmarks.

Match actual artifacts and installed APIs. In particular, shared navigation consumes `org.jetbrains.androidx.navigation3:navigation3-ui` through `libs.multiplatform.navigation3.ui`; the separate Android `navigation3` version alias does not establish its version. Read the catalog/module declaration and, when necessary, dependency output before copying a sample or making a compatibility claim. Apply [KMP/CMP checks](multiplatform.md#changes-to-shared-code) without incidental upgrades.

## Durable memory

| Location | Owns | Maintenance |
| --- | --- | --- |
| `AGENTS.md`, `Human.md`, linked guides | Current repository rules and human workflow | Keep universal rules short; update the authoritative rule instead of appending contradictory instructions. |
| `handoff.md` | Active behavior, approved decisions, source checkpoint, latest evidence/limits and next action | Replace stale status. Aim for about one page; archive history before it becomes another transcript. |
| `history/` | Previous evidence and superseded decisions | Read only for a specific question; never inherit old permissions. Git also preserves prior versions. |
| Private ignored `Notes/Todo.md` | Succinct user backlog and post-feature work | Preserve privacy; a public clone must still have usable tracked context. |

Repository files are the portable memory for new chats and collaborators. Do not assume personal agent memory is available, current, or shared with others; do not copy private machine configuration into the repository. Read current source/Git after compaction, too. If the user commits during a check, compare content before deciding whether evidence is invalidated; a SHA change alone does not mean the code changed.

Preserve Human.md feedback. Label proposals separately from accepted instructions. Record only facts that would change future scope, implementation, verification or decisions. Temporary trial-and-error, unchanged test counts and routine command noise do not belong in every handoff.

## Tools, skills and research

Use `rg`/targeted reads and existing build scripts first. Prefer official Android/Kotlin/library documentation for APIs. Match installed versions; a current online example may target a newer release. For research, open the source, record its date or living-doc status, separate evidence from opinions, and describe where a recommendation is an inference.

Select skills for a demonstrated gap: Android CLI for SDK/docs/tooling; inset/performance skills for actual inset/performance defects; native build guidance when native tooling is needed. A testing-setup skill does not authorize new frameworks/cases during the feature phase. A native-debugging skill does not authorize device use. User instructions override skill examples.

Official Kotlin skills, IDE MCP and library lookup tools are options when navigation/inspections or current API knowledge repeatedly fail. Evaluate the specific benefit, trust and platform fit first; do not install a large skill collection, add a desktop target solely for previews, or replace the working convention plugins to match a tool's default project. No new skill package is needed merely to duplicate these guides. Use one agent by default; delegate only when explicitly authorized, with isolated task/output ownership.

No workflow or model guarantees error-free code. Useful completion claims say what changed, which checks actually ran or were cached, what they cover, and what remains pending. Shared Lists follow their [agreed feature design](../features/shared-watchlists.md). New scope still requires agreement; live provisioning, recovery and the onboarding tour remain separate. See [public-source guidance](open-source.md).

# UI code audit and review-tool research

12 November 2026. Reviewed commits `366082c`, `0b3cda1`, `f677057`, current film/contributor adapters and their callers, plus the existing workflow. This is a focused audit of the recent UI work, not certification of the entire codebase. Existing user title/formatting edits are excluded from findings and preserved.

## Findings and causes

| Evidence | What I got wrong | Correction |
| --- | --- | --- |
| `0b3cda1` put the contributor in `FilmMetadata` beside the year, before the synopsis. The request specified moving it off the poster, not this exact placement. | I treated a visual decision as implementation detail and chose a convenient existing row. | Owner confirmed bottom-right; `f677057` supplies a separate footer. Ambiguous placement now requires a question before dependent edits. |
| The same commit added `(Int) -> (@Composable () -> Unit)?` to manufacture contributor content for two call sites. | I removed small repetition by introducing a harder-to-read function-producing function. Generic appearance was mistaken for useful reuse. | Removed in `f677057`; simple slots remain at their actual call sites. Review must compare the abstraction with a direct implementation. |
| `FilmContributorButton` reused a 48 dp icon button with 12 dp padding on each side for a 32 dp avatar, plus the old poster-badge border. | I followed the component name without tracing its constraints: only 24 dp remained for the child. | `f677057` uses a dedicated avatar control with 8 dp padding and a scalable minimum target; removes the poster-only decoration. |
| After the last row-overlay consumer moved, `MovieListRow` retained its `posterOverlay` parameter and a Box solely to host it. No current caller supplies that slot. | I reviewed the added path without checking whether the old path was now dead. | Removed the unused row slot/Box in this audit. The image uses the same direct modifier pattern as `MoviePosterCard`; grid overlays still have actual edit/Add consumers. |
| `posterWidth` selects both width and ratio-vs-text-height sizing, while `compact` also affects dimensions and text limits. | Incremental options made the row contract harder to infer. This predates the footer fix. | Documented as remaining design debt. No speculative sizing enum or layout rewrite during this audit: changing legacy search sizing needs its own agreed scope and physical review. |
| Static/diff/docs checks ran, but the gallery previously lacked the production contributor composition and no physical review was available. | Those checks could not establish visual quality. More research or another successful build would not establish placement correctness. | Gallery footer examples now exist; physical acceptance remains pending. Completion reports must distinguish mechanical/source review from visual evidence. |

The underlying process failure was insufficient design reasoning and incomplete end-to-end source review. Extra files, interfaces, prompts or installed skills alone would not have corrected those decisions. The practical response is the short [required code review](../code-review.md), linked from startup instructions, with a fast mechanical check and an explicit manual simplicity pass.

The existing layers are not all accidental complexity: the feature owns contribution semantics, presentation maps persisted avatar colours, and the design system owns geometry/interaction. Removing those boundaries merely to shorten the call chain would mix responsibilities. The new avatar button retains press/focus/accessibility handling; those lines are not a reason to delete useful behavior.

## Primary-source research

Sources opened 12 November 2026. Older foundational essays are dated below; tool/skill repositories and documentation are living sources. These are adopted principles and project judgments, not evidence of a guaranteed defect-prevention rate.

| Source | Finding and application |
| --- | --- |
| Robert C. Martin: [SRP](https://blog.cleancoder.com/uncle-bob/2014/05/08/SingleReponsibilityPrinciple.html) (8 May 2014), [SOLID relevance](https://blog.cleancoder.com/uncle-bob/2020/10/18/Solid-Relevance.html) (18 October 2020) | Responsibility follows reasons to change; small contracts and consistent implementations matter. Apply all five principles as questions about coupling/behavior, not quotas for interfaces/helpers. |
| Google: [what reviewers look for](https://google.github.io/eng-practices/review/reviewer/looking-for.html), [small changes](https://google.github.io/eng-practices/review/developer/small-cls.html) | Review overall design, user impact and unnecessary generality; keep changes understandable. Local testing restrictions override generic advice to add tests. |
| Kotlin: [API simplicity](https://kotlinlang.org/docs/api-guidelines-simplicity.html), [coding conventions](https://kotlinlang.org/docs/coding-conventions.html) | Limit concepts and exposed API; use existing types and readable Kotlin. No incidental explicit-API migration or repository-wide formatting. |
| AndroidX: [component API guidance](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md) (source dated 19 July 2023) | A component must justify its API/support burden. Use ordinary Compose composition when a new generic abstraction adds little value. Layout/a11y research remains in [UI guidelines](../../design/ui-guidelines.md). |

## Skills and tools evaluated

Used the installed `find-skills` instructions, directory discovery and direct author sources. Popularity is only discovery evidence; source inspection and project fit decide adoption. Counts below are displayed snapshots, not verified usage or quality measurements. No packages/plugins were installed and no external instructions were copied verbatim.

| Candidate / source | Assessment and decision |
| --- | --- |
| [Addy Osmani: code-review-and-quality](https://github.com/addyosmani/agent-skills/blob/main/skills/code-review-and-quality/SKILL.md), [directory](https://skills.sh/addyosmani/agent-skills/code-review-and-quality) (86.3K displayed installs; 116K repository stars) | Best general review reference found: correctness, readability, architecture, security and performance. Its full test/release expectations are broader than this feature phase. Adapted the review dimensions into the local checklist; installation remains optional. |
| [Anthropic: code-simplifier](https://github.com/anthropics/claude-plugins-official/blob/main/plugins/code-simplifier/agents/code-simplifier.md) (official plugin repository, about 47K stars; individual install count unavailable) | Useful scoped, behavior-preserving simplification and clarity-over-brevity guidance. It is a Claude Code agent/plugin with JS/React instructions, not a drop-in Kotlin/Codex skill. Used as a reference, not installed or ported wholesale. |
| [obra: requesting-code-review](https://github.com/obra/superpowers/blob/main/skills/requesting-code-review/SKILL.md), [directory](https://skills.sh/obra/superpowers/requesting-code-review) (279.2K displayed installs; about 316K repository stars) | Clear review context and prioritised findings are useful. Its required reviewer delegation conflicts with this project's single-agent default. Not adopted as an executable workflow; no subagent spawned. |
| [voku: clean-code-principles](https://skills.sh/voku/agent-skills/clean-code-principles) (4 displayed installs) | Exact SOLID keyword match, but source retrieval failed and the directory only exposed an overview. Insufficient inspection to recommend; not adopted. |
| Installed `jetpack-compose-performance` | Use for demonstrated layout/performance questions with its matched reference. It cannot judge aesthetics or certify general Clean Code. No broader skill pack needed to repeat our own rules. |
| [Detekt](https://detekt.dev/docs/introduction/compatibility/), [Compose Rules](https://github.com/mrmans0n/compose-rules) | Useful Kotlin/Compose static analysis candidates. Published Detekt table lists 2.0.0-alpha.6 against Kotlin 2.4.10 / AGP 9.3.1; this project consumes Kotlin 2.4.21 / AGP 9.3.3. That difference is not proof of incompatibility, but this stack was not validated here. Defer plugin adoption to a separately verified tooling change; no untested dependency upgrade or blanket suppressions. |
| [ktlint](https://github.com/ktlint/ktlint) | Formatting/lint can remove mechanical review noise; it cannot settle responsibility or visual hierarchy. Defer a pinned, convention-plugin-integrated configuration until a scoped tooling pass; do not reformat user edits now. |
| [Konsist](https://docs.konsist.lemonappdev.com/) | Architecture assertions can enforce selected structural rules through tests. New architecture cases are deferred under the feature-phase policy. |

Access limits: the old Uncle Bob principles index and old Pinterest-hosted ktlint URLs failed; the author’s SOLID essay and current ktlint repository were accessible. The direct voku source was unavailable. These limits are reflected in the decisions above, rather than treating search summaries as a source review.

## Applied controls and limits

- Added `scripts/review-code.sh`: offline, read-only patch checks plus selected added-import boundaries, with staged/unstaged/untracked and explicit starting-commit coverage. It reports locations without printing source. It deliberately cannot claim semantic SOLID/behavior/UI validation.
- Added the canonical review checklist; workflow requires it after coherent changes and review fixes. No per-task audit bureaucracy, autonomous delegation, auto-formatting or build/device policy change.
- Removed the row's unused overlay API/wrapper. No new feature tests, library versions, backend or layout decisions introduced. Existing user edits remain untouched.
- Latest verification and remaining physical acceptance are recorded in the [handoff](../handoff.md). No process or tool guarantees that this failure never recurs; evidence-backed review makes the same failure easier to detect before handoff.

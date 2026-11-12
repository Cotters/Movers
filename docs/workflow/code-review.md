# Review every code change

Run after each coherent code change and again after review fixes, before committing or handing back. This is a source review, not an automatic build gate. Use [UI review](../design/ui-guidelines.md) for layout decisions and [testing](testing.md) for permitted verification.

## Routine

1. Record the task's starting commit and existing user edits. Read affected functions and callers before choosing an implementation. Resolve consequential unknowns, including ambiguous placement.
2. Implement the smallest coherent change using existing patterns. For each new abstraction or option, identify the current requirement it serves and why a direct implementation is less clear. Keep that reasoning in the review, not boilerplate comments throughout the source.
3. Run `./scripts/review-code.sh`. If the task already has commits, pass its starting commit: `./scripts/review-code.sh <task-start-commit>`. It includes staged, unstaged and non-ignored untracked code/config files. Preserve user edits and distinguish their ownership.
4. Read the complete affected functions and their direct callers against the questions below. Trace an actual value/event through the layers and, for UI, the constraints/padding to the rendered child. Remove obsolete paths left by the change. Review fixes get the same pass.
5. Resolve substantive findings before completion. Report the material simplification, actual checks and remaining limits briefly. Record only consequential unresolved debt in the handoff/Todo; do not create an audit document or a checklist transcript for every edit.

## Questions requiring judgment

| Lens | Ask of the actual changed code |
| --- | --- |
| Correctness | Does it match the agreed behavior? Are errors, cancellation, pending writes and persisted relationships preserved? |
| Simplicity | Can a reader follow the flow directly? Can an unused slot, factory, switch, wrapper or duplicated state be removed? Is an extraction hiding detail or just adding another hop? |
| Responsibility | Does each component own a coherent concern? Do geometry, domain transformations and persistence remain with their existing owners? |
| Contracts | Are names, defaults, null/empty behavior and side effects clear? Does adding an option unexpectedly change another aspect of behavior? |
| UI | Are hierarchy, alignment and spacing agreed? Are image ratio, natural text growth, touch areas and semantics intact? Gallery approval and compilation cannot prove screen quality. |
| Integration | Have all callers and relevant platform source sets been considered? Are public-source privacy and actual dependency versions respected? |

## SOLID applied to Movers

These are design questions, not requirements to add classes/interfaces or inheritance to composable functions.

- **Single responsibility:** group code by reasons to change. A ViewModel coordinates membership; a composable measures and renders it. Do not extract every line into a separate helper.
- **Open/closed:** extend existing contracts or slots for demonstrated variation. Avoid speculative frameworks and configuration knobs for hypothetical future consumers.
- **Liskov substitution:** adapters honor their contract's results, failures, cancellation and persistence semantics. Swapping a database provider must not silently change those guarantees.
- **Interface segregation:** consumers receive the operations/data they need. The design system accepts display values and callbacks; it does not require film, participant or backend models.
- **Dependency inversion:** features depend on existing provider-neutral contracts; internal adapters and Koin supply implementations. An interface per concrete class is not a goal.

Prefer explicit names, straightforward control flow and focused changes. Preserve helpful abstraction and accessibility; fewer lines alone do not establish cleaner code. Formatting and lint are useful mechanics, not a substitute for this review.

## Tool limits

The fast script uses Git and Python 3, performs no build/network/device action, and changes no files. It checks patch whitespace, added conflict markers and selected added Kotlin imports: platform-only imports in commonMain, and domain/data/DI imports in the design system. It prints locations, never source lines. These are textual checks, not Kotlin parsing or a complete dependency graph; fully qualified references and semantic architecture still need review. Success is deliberately labelled **mechanical checks passed**, never **SOLID passed**.

Run existing suites only under the build policy; add no new feature test cases without authorization. Tool/skill research, adoption decisions and the motivating source audit live in the [12 November audit](history/2026-11-12-ui-code-audit.md).

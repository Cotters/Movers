# Review every code change

Run during every coding task, after each coherent change/review fix and **before every code commit**. Formatting, mechanical checks and source review are separate requirements. This adds no automatic build gate. Use [UI review](../design/ui-guidelines.md) for layout decisions and [testing](testing.md) for permitted verification.

## Routine

1. Record the task's starting commit and existing user edits. Read affected functions and callers before choosing an implementation. Resolve consequential unknowns, including ambiguous placement.
2. Implement the smallest coherent change using existing patterns. For each new abstraction or option, identify the current requirement it serves and why a direct implementation is less clear. Keep that reasoning in the review, not boilerplate comments throughout the source.
3. Format the task-owned Kotlin changes using the Android Studio tools below; inspect imports and available diagnostics. Run `./scripts/review-code.sh`. If the task already has commits, pass its starting commit: `./scripts/review-code.sh <task-start-commit>`. It checks staged and working-tree snapshots separately, plus non-ignored untracked code/config files. Preserve user edits and distinguish their ownership.
4. Read the complete affected functions and their direct callers against the questions below. Trace an actual value/event through the layers and, for UI, the constraints/padding to the rendered child. Remove obsolete paths left by the change. Review fixes get the same pass.
5. Before committing, inspect the staged diff and ensure the checks/review cover that exact content. Re-run affected checks after formatting or fixes; do not claim an earlier pass covers new edits. Resolve substantive findings before completion. If required formatting/diagnostics cannot run, report what is pending and why; do not claim the code commit meets the gate. Report the material simplification, actual checks and remaining limits briefly. Record only consequential unresolved debt in the handoff/Todo; do not create an audit document or a checklist transcript for every edit.

## Formatting and static analysis

- The tracked [`.editorconfig`](../../.editorconfig) selects Kotlin official style, four spaces, UTF-8/LF and a final newline for `.kt`/`.kts`. Keep named arguments and slot calls readable; avoid semicolon-packed statements and dense expressions. Group related declarations/overloads by responsibility. Automatic reformatting does not reorganize responsibilities or prove SOLID.
- Prefer the running Android Studio IDE/bridge when available: **Code → Reformat Code** on the changed selection/lines, then **Optimize Imports** and inspect diagnostics for affected files. Kotlin recommends the **Incorrect formatting** inspection. Do not blindly run Code Cleanup, rearrange initialization order, or reformat the repository. Inspect the resulting diff for accidental behavior changes and unrelated churn.
- CLI equivalent: `./scripts/format-kotlin.sh --check path/to/File.kt`. The default is validation only. Use `--write` on explicit task-owned files, review the diff, then run `--check`. Paths are checkout-relative or absolute inside the checkout; directories and external files are rejected. It uses Studio's installed formatter and `.editorconfig`, without Gradle or devices. Set `MOVERS_STUDIO_FORMATTER` to the format launcher on other installations; no machine-specific paths are tracked.
- The CLI formats whole named files, so use selected-line IDE formatting when a file also has unrelated user edits. Preserve those edits. Never stop/restart the user's IDE to obtain its lock. If Studio is busy and no IDE bridge is available, complete independent checks and explicitly leave formatter verification pending. Do not silently downgrade that to a formatting pass.
- Before every code commit: formatter/import/diagnostic review, `review-code.sh`, and the manual questions below must cover the commit. Documentation-only tasks use `verify-docs.sh` and diff checks; shell/Python tooling also gets appropriate syntax/source checks. Do not run Kotlin formatting on untouched app files for a documentation task.
- Gradle's existing `lintDebug` is retained in `scripts/verify.sh`. Run compiler-backed lint/tests only under the build policy, using the smallest confirmed task scope; no full build just to format code. No `ktlint`, `detekt` or IDE-inspection result may be claimed unless that tool actually ran. New lint dependencies/profiles require a scoped, validated tooling change.

References checked 12 November 2026: [Kotlin IDE style/inspections](https://kotlinlang.org/docs/coding-conventions.html#configure-style-in-ide), [JetBrains reformat/import tools](https://www.jetbrains.com/help/idea/reformat-and-rearrange-code.html), [CLI validation and IDE lock](https://www.jetbrains.com/help/idea/command-line-formatter.html), [EditorConfig support](https://www.jetbrains.com/help/idea/editorconfig.html). Shared EditorConfig selects formatting; IDE-local commit/save settings are not silently enabled for the user.

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

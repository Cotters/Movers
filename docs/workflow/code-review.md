# Review every code change

Run during every coding task, after each coherent change/review fix and **before every code commit**. Formatting, mechanical checks, source-only Kotlin lint and source review are separate requirements. This adds no automatic app-build gate. Use [UI review](../design/ui-guidelines.md) for layout decisions and [testing](testing.md) for permitted verification.

## Routine

1. Record the task's starting commit and existing user edits. Read affected functions and callers before choosing an implementation. Resolve consequential unknowns, including ambiguous placement.
2. Implement the smallest coherent change using existing patterns. For each new abstraction or option, identify the current requirement it serves and why a direct implementation is less clear. Keep that reasoning in the review, not boilerplate comments throughout the source.
3. Format the task-owned Kotlin changes using the Android Studio tools below; inspect imports and available diagnostics. Run **`./scripts/check-code.sh`**, which runs the mechanical review script followed by real Kotlin lint and propagates either failure. If the task already has commits, pass its starting commit: `./scripts/check-code.sh <task-start-commit>`. The mechanical phase checks staged and working-tree snapshots separately, plus non-ignored untracked code/config files. Detekt scans current source, so inspect staged content for differences before committing. Preserve user edits and distinguish their ownership.
4. Read the complete affected functions and their direct callers against the questions below. Trace an actual value/event through the layers and, for UI, the constraints/padding to the rendered child. Remove obsolete paths left by the change. Review fixes get the same pass.
5. Before committing, inspect the staged diff and ensure the checks/review cover that exact content. Re-run affected checks after formatting or fixes; do not claim an earlier pass covers new edits. Resolve substantive findings before completion. If required formatting/diagnostics cannot run, report what is pending and why; do not claim the code commit meets the gate. Report the material simplification, actual checks and remaining limits briefly. Record only consequential unresolved debt in the handoff/Todo; do not create an audit document or a checklist transcript for every edit.

## Formatting and static analysis

- The tracked [`.editorconfig`](../../.editorconfig) selects Kotlin official style, four spaces, a 120-character line limit matching lint, UTF-8/LF and a final newline for `.kt`/`.kts`. Keep named arguments and slot calls readable; avoid semicolon-packed statements and dense expressions. Group related declarations/overloads by responsibility. Automatic reformatting does not reorganize responsibilities or prove SOLID.
- Prefer the running Android Studio IDE/bridge when available: **Code → Reformat Code** on the changed selection/lines, then **Optimize Imports** and inspect diagnostics for affected files. Kotlin recommends the **Incorrect formatting** inspection. Do not blindly run Code Cleanup, rearrange initialization order, or reformat the repository. Inspect the resulting diff for accidental behavior changes and unrelated churn.
- CLI equivalent: `./scripts/format-kotlin.sh --check path/to/File.kt`. The default is validation only. Use `--write` on explicit task-owned files, review the diff, then run `--check`. Paths are checkout-relative or absolute inside the checkout; directories and external files are rejected. It uses Studio's installed formatter and `.editorconfig`, without Gradle or devices. Its config/cache/log directories are isolated under ignored `build/quality/studio-formatter`, so it can run alongside the user's IDE. Explicit `STUDIO_PROPERTIES` overrides are preserved; serialize formatter invocations sharing this cache. Set `MOVERS_STUDIO_FORMATTER` to the format launcher on other installations; no machine-specific paths are tracked.
- The CLI formats whole named files, so use selected-line IDE formatting when a file also has unrelated user edits. Preserve those edits. Never stop/restart the user's IDE. If the isolated formatter or IDE bridge cannot run, complete independent checks and explicitly leave formatter verification pending. Do not silently downgrade that to a formatting pass.
- Before every code commit: formatter/import/diagnostic review, `check-code.sh`, and the manual questions below must cover the commit. The component scripts remain available for diagnosis; a mechanical-only pass does not satisfy the full gate. Documentation-only tasks use `verify-docs.sh` and diff checks; shell/Python tooling also gets appropriate syntax/source checks. Do not run Kotlin formatting on untouched app files for a documentation task.
- For Swift host edits, run Xcode's `xcrun swift-format lint --strict` on the affected source with the tracked `.swift-format`; inspect imports and compiler diagnostics when a build is authorized. `verify-ios.sh` includes that check for the app entry point. Kotlin lint does not analyze Swift. See [iOS verification/release](ios-release.md).
- Gradle's existing `lintDebug` is retained in `scripts/verify.sh`. Run compiler-backed lint/tests only under the build policy, using the smallest confirmed task scope; no full build just to format code. No `ktlint`, `detekt` or IDE-inspection result may be claimed unless that tool actually ran. New lint dependencies/profiles require a scoped, validated tooling change.

References checked 12 November 2026: [Kotlin IDE style/inspections](https://kotlinlang.org/docs/coding-conventions.html#configure-style-in-ide), [JetBrains reformat/import tools](https://www.jetbrains.com/help/idea/reformat-and-rearrange-code.html), [CLI validation and IDE lock](https://www.jetbrains.com/help/idea/command-line-formatter.html), [EditorConfig support](https://www.jetbrains.com/help/idea/editorconfig.html). Shared EditorConfig selects formatting; IDE-local commit/save settings are not silently enabled for the user.

### Kotlin code-smell lint

`./scripts/lint-kotlin.sh` runs the isolated Gradle task `./gradlew -p tools/lint lintKotlin`. It uses pinned Detekt CLI 2.0.0-alpha.6 on JDK 17 in **light/source-only** mode with Kotlin language level 2.4. No Android SDK, app target compilation, tests, emulator or simulator is involved. First use resolves the tool from Maven Central; later runs use Gradle's cache. Its separate classpath avoids changing the app's Kotlin/AGP plugins.

The scan covers tracked and non-ignored untracked `.kt`/`.kts` across shared/native/Android code and build logic. Ignored credentials, private Notes and generated build output are not selected. [Configuration](../../config/detekt.yml) builds on Detekt defaults, enables unnecessary-parentheses checks, and uses documented Compose naming/parameter/numeric-literal allowances. The profile extends standard test exclusions to Android KMP host/device tests, permits scenario names on `@Test`, excludes required overrides from method counts, retains grouping for unclear precedence, and permits early guard returns. Application complexity limits remain unchanged; these choices keep lint aligned with the existing contracts and test conventions. The naming pattern also accepts exactly `MainViewController`, preserving the [documented Compose/Swift factory](https://kotlinlang.org/docs/multiplatform/compose-swiftui-integration.html); other ordinary function names retain lower camel case. **Warning or Error severity fails the command.** No baseline, ignore-failure switch or automatic corrections are configured. Fix genuine findings; justify an actual false-positive exception at its narrowest scope rather than disabling a rule to hide debt.

Reports: `tools/lint/build/reports/detekt.html` and `detekt.xml` (ignored). A nonzero exit with findings is a lint failure, not a clean result. Do not describe the codebase as warning-free while findings remain. Existing debt needs an explicit cleanup scope; retain the failing report rather than silently accepting or suppressing it.

Cleanup is now authorized and tracked in the [cleanup plan](kotlin-lint-cleanup.md). Intermediate cleanup commits retain their reported remaining findings; they are not lint-clean feature commits. The cleanup reached zero findings on 13 November 2026. CI now has an independent `Kotlin source lint` job running `scripts/lint-kotlin.sh` on public source, without app compilation or backend credentials; reports are retained after failures. Hosted execution remains unverified until the workflow runs. Version/target-SDK advisories from Android lint are tracked separately; do not disable them or migrate dependencies incidentally.

Coverage limit: light analysis cannot run rules requiring type resolution, including Detekt's [UnusedImport](https://detekt.dev/docs/rules/style/#unusedimport). IDE diagnostics/import inspection and authorized compiler-backed Android lint remain required for their separate coverage; a source-only pass is not “zero IDE/compiler warnings.” [CLI mode/severity](https://detekt.dev/docs/gettingstarted/cli/), [Compose configuration](https://detekt.dev/docs/introduction/compose/) and [standalone Gradle task](https://detekt.dev/docs/gettingstarted/gradletask/) were checked against the actual resolved tool. This gate complements manual architecture/SOLID review.

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

For broad operation boundaries, use the existing shared `recoverableResult` policy rather than duplicating cancellation guards or using plain `runCatching` for suspending work. Kotlin `runCatching` captures every `Throwable`; the policy deliberately rethrows cancellation and fatal errors. Callers still own typed domain errors, causes, user messages and `finally` cleanup. A warning fix must preserve semantics: conditional lambdas, infix operators and labelled/non-local returns need full-function review, not blind syntax removal.

Prefer explicit names, straightforward control flow and focused changes. Preserve helpful abstraction and accessibility; fewer lines alone do not establish cleaner code. Formatting and lint are useful mechanics, not a substitute for this review.

## Tool limits

The fast script uses Git and Python 3, performs no build/network/device action, and changes no files. It checks patch whitespace, added conflict markers and selected added Kotlin imports: platform-only imports in commonMain, and domain/data/DI imports in the design system. It prints locations, never source lines. These are textual checks, not Kotlin parsing or a complete dependency graph; fully qualified references and semantic architecture still need review. Success is deliberately labelled **mechanical checks passed**, never **SOLID passed**.

Run existing suites only under the build policy; add no new feature test cases without authorization. Tool/skill research, adoption decisions and the motivating source audit live in the [12 November audit](history/2026-11-12-ui-code-audit.md).

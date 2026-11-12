# Kotlin lint cleanup plan

Owner chose **finish lint setup; plan cleanup next** on 12 November 2026. Application cleanup has not started. Setup uses strict source-only Detekt; its reported failure is retained, with no suppression baseline. Do not claim the codebase is warning-free.

## Current evidence

`JAVA_HOME=$(/usr/libexec/java_home -v 17) ./scripts/lint-kotlin.sh` resolves/runs the tool and fails with **656 findings across 129 files**:

| Rule group | Findings |
| --- | ---: |
| Long lines | 397 |
| Magic numbers | 124 |
| Unnecessary parentheses | 22 |
| Broad exception catches | 20 |
| Cyclomatic complexity | 17 |
| Other naming, structure and style rules | 76 |

The new lint build scripts have no findings. Full reports are ignored under `tools/lint/build/reports/`; the latest private log is `/private/tmp/movers-kotlin-lint-final.log`. Reports describe a source snapshot, not a permanent debt count. Type-dependent rules such as unused imports are outside light-mode coverage.

## Proposed sequence

1. **Confirm findings and formatting access.** Re-read the current report and Git state. Inspect representative Compose factories, tests and numeric values before changing conventions; some findings may need narrowly justified configuration rather than code churn. Resolve IDE formatting access without closing the user's running IDE. Preserve existing user edits.
2. **Mechanical fixes in small commits.** Start with final newlines, actual redundant parentheses, wildcard/unused imports and wrapping. Use Studio formatting/import tools for task-owned files/lines. Review each diff; do not rewrite strings or initializers merely to satisfy a line limit.
3. **Constants and focused refactors.** Name meaningful domain/protocol values; retain intentional UI/test literals where appropriate. Address long/complex functions and broad/swallowed exceptions one concern at a time. Preserve cancellation, retries, error messages, persisted relationships, gesture behavior and module boundaries. Do not extract meaningless helpers or replace real recovery logic with narrower catches without evidence.
4. **Verify the cleaned source.** Run strict lint after each slice and report any remaining findings honestly. For the behavior-sensitive refactors, explain and run the smallest relevant existing host checks; use the existing broader Android/iOS verification when the cumulative scope warrants it. Add no new feature tests. Device/UI acceptance remains physical and user-owned.
5. **Enforce clean results in CI.** After the approved profile is clean, add the source-only lint job/step to CI and confirm its reports/failure behavior. No broad suppressions, ignore-failure flags, generated baseline, or repository-wide auto-format to manufacture green.

Completion requires zero findings for the agreed Detekt profile, resolved relevant IDE/type-aware warnings with their checks actually run, preserved behavior and user edits, and evidence scoped to the actual targets. A Detekt pass alone does not prove SOLID or visual quality. No app release, dependency migration or backend work is part of this cleanup.

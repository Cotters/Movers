# Kotlin lint cleanup plan

Owner authorized cleanup on 12 November 2026, including the existing working changes. Cleanup is in progress. Setup uses strict source-only Detekt; its reported failure is retained, with no suppression baseline. Do not claim the codebase is warning-free.

## Starting evidence

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

## Progress

- Formatting: 106 lint-flagged files processed by the isolated Android Studio formatter. A Kotlin lexer comparison verified unchanged code/string tokens in those files. The user's pending film title styling is included separately as authorized.
- Profile review: modern KMP test folders, annotated test names and required interface overrides follow the existing/default test and contract conventions. Guard clauses and readability parentheses remain permitted. Complexity limits are unchanged.
- Latest scan after this slice: **208 findings**, with behavior-sensitive refactoring and production constants still to address. No application build/tests or device verification for the formatting-only slice.

- Naming/syntax slice: value types now have matching files, constants use Kotlin names, explicit mapper guards preserve validation, and redundant syntax/wildcard imports are removed. Studio validation passes all 33 touched Kotlin files. Existing Android host suite succeeds after correcting conditional callback lambdas; no cases were added. Source lint remains failing with **182 findings**.

- Document slice: separate encoding, quoted-table reader, column mapping, reference parsing and export codecs. Preserve partial recovery and limits; JSON shape errors now catch `IllegalArgumentException`, preserving the root cause. Eight files pass Studio formatting, existing watchlist/shared host tests pass, strict lint reports **157 remaining**. No dedicated parser suite exists; that coverage remains deferred.

- Constants slice: name existing timing, protocol and validation values, centralize the import batch size, and retain exactly the authorized retired database versions. Studio formatting validates 15 touched Kotlin files; strict lint reports **127 remaining**. No extra app build for constant-only edits; cumulative verification follows structural work.

- Shared controls: separate actual styling/decoration/accessibility responsibilities without changing approved layout or behavior. Six changed files pass Studio formatting and the complete existing Android host suite passes. Strict lint reports **120 remaining**.

- Membership/migration slice: explicit observed-list values and focused existing migration setup/assertions; SQL literals remain identical. Six Kotlin files pass Studio formatting; affected host suites pass with no new cases. Strict lint reports **101 remaining**.

- DAO/cache slice: segregate memberships, pending receipts and activity DAOs; isolate shared cache persistence. Schema export, queries and transaction boundaries are unchanged. Twelve Kotlin files pass Studio formatting; affected database/repository/shared host suites pass. Strict lint reports **98 remaining**.

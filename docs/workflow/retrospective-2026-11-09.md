# Retrospective: AI-assisted Android/iOS work

Reviewed 9 November 2026 against this chat, source at `e407ed6` plus the existing immediate-membership working tree, and the references below. This review changes workflow/documentation and adds a small documentation check. It does not authorize new app features, tests, dependencies, agent installations or device interaction.

## What went well

- **Consequential choices were explicit.** Koin, local device identity/Josh, deferred onboarding, the one-time legacy reset and separate future collaboration scope were agreed rather than inferred. The later immediate-add UX was a legitimate change to the earlier agreed confirmation flow.
- **The architecture remained useful across platforms.** Shared features use the existing contracts, convention plugins, typed navigation and entry-scoped state. The list repository has a separate provider-neutral storage API with an internal Room adapter. This enables future backend replacement without adding a provider SDK to screens.
- **Persistence and real integration evidence caught meaningful faults.** The preserving 7→8 migration, populated/reopened storage and atomic rollback checks protected existing data. Real-app host UI tests caught resource packaging, vector parsing and lifecycle failures that compilation missed. The latest app gate passed 92 host tests; iOS compilation and historical native smoke results were reported separately.
- **Authorization and private material stayed scoped.** Earlier device exceptions were explicit; the latest no-emulator instruction governs current work. Local credentials and private Notes remain ignored. The user's commits and working-tree changes were preserved.

Retain these strengths. The existing checks remain valuable while new-case expansion is deferred to the post-feature review.

## What I got wrong or made inefficient

| Observed problem | Consequence | Applied correction / owner |
| --- | --- | --- |
| I tried several layout, clock/dispatcher and teardown changes before collecting the decisive diagnostics. | Repeated UI runs delayed a small UX change. A timeout was too often treated as a cause instead of a symptom. | Agent: exact failure/UI state first; one discriminating experiment; after two failed hypotheses switch to diagnosis and a useful update. See [development](development.md#debugging-without-repeated-guessing). |
| I did not distinguish isolated behavior from suite-order behavior early enough. | Single-case successes did not explain later catalogue stalls. | Agent: compare the same case alone and after its predecessor, then inspect shared lifecycle/queue state. The confirmed outer-rule cleanup is documented in [testing](testing.md#host-ui-regression-gate). |
| Some Gradle invocations overlapped or repeated without a new substantive question. | Shared outputs and overwritten filtered-test reports made evidence harder to attribute. No report corruption was established. | Agent: serialize builds per checkout, use distinct logs, and run final gates after edits settle. Preserve the scope/source of each result. |
| The handoff grew to about 3,700 words, repeating old behavior and superseded permissions. | New contexts had to distinguish active instructions from a transcript. | Repository memory: compact [current handoff](handoff.md), with the complete previous record in [history](history/2026-11-09-implementation.md). History is optional lookup, never current authorization. |
| Development guidance named Nav 3 1.2.1 from the Android catalog alias. Shared modules actually consume JetBrains' 1.1.1 artifact. | An agent could copy an API for the wrong library version despite reading the guide. | Architecture guidance: inspect the module's consumed alias/artifact and installed source. No dependency upgrade was made. |
| Test work and workflow ceremony became too expensive for the user's current feature phase. | The user had to restate the desired speed/coverage tradeoff. | Current rules: maintain/run existing cases; no new cases, especially UI, unless requested. The broader review stays in private Todo. Clear follow-ups proceed without a fresh approval cycle. |
| Broad document reads and repeated narrative updates consumed context without always resolving uncertainty. | More context and time, with less useful progress information. | Agent: targeted reads; updates state evidence, uncertainty and the next distinguishing check. Docs-only work gets a fast local-link check instead of app rebuilds. |

These are execution/process findings. A missing tool or additional skill would not, by itself, have supplied the observations I delayed collecting. Successful eventual verification does not make the avoidable loops efficient.

## Research and adoption decisions

Dated articles below fall within 9 May–9 November 2026. Living documentation is labelled as such; it has no invented publication date. Community comments describe individual experience and disagreement, not Android guarantees or a representative survey. Sources were opened, not accepted from search snippets.

| Source / status | Useful finding | Application here |
| --- | --- | --- |
| [Google: Inside Android Skills](https://android-developers.googleblog.com/2026/08/android-skills-philosophy.html?m=1), 6 Aug 2026 | Specialized, evaluated skills address concrete knowledge gaps; broad collections add context cost. | Select a skill for the task; keep official docs available. No bulk skill installation. |
| [OpenAI: Rethinking skills and prompts](https://developers.openai.com/blog/rethinking-skills-and-prompts-for-gpt-6-astra), 11 Sep 2026 | Long universal reading instructions and overlapping skills can add unnecessary context. | Short current rules, task-specific links and a compact handoff. Apply the idea without depending on a particular model. |
| [OpenAI: Iterating Development Workflows](https://developers.openai.com/cookbook/examples/codex/iterating-development-workflows-with-codex), 3 Aug 2026 | Durable context should retain material decisions; its example file structure is optional. | Reuse the existing guides/handoff instead of adding mandatory GOALS/PLANS/PROMPTS files. |
| [OpenAI: Custom Code Review rules](https://developers.openai.com/blog/custom-code-review-rules-for-codex), 20 Jul 2026 | Narrow, consequential invariants guide useful reviews; deterministic mechanics belong in checks. | Preserve data/cancellation/boundary rules and add local documentation-target validation to CI. No automated review service was enabled. |
| [Kotlin: AI tools for development](https://kotlinlang.org/docs/ai-for-development.html), living docs read 9 Nov | Kotlin-specific context and explicit platform acceptance help agents; IDE MCP, library lookup and skills serve different gaps. | Document Android/iOS build coverage and common/platform ownership. Evaluate tools only for a demonstrated need; existing device restrictions still apply. |
| [Kotlin: Share code between platforms](https://kotlinlang.org/docs/multiplatform/multiplatform-share-on-platforms.html), living docs read 9 Nov | Common and platform source sets have distinct responsibilities. | Shared logic/UI first, small native integrations; verify affected targets without forcing all integrations into common code. |
| [Android developers' Compose skills discussion and comments](https://www.reddit.com/r/androiddev/comments/1tjfn3t/i_got_tired_of_ai_agents_breaking_my_compose_code/), displayed age five months | Comments correct the post's blanket ban on StateFlow assignment, distinguishing read/modify/write from independent assignment. | Record rationale and concurrency conditions; avoid importing a generic strict-MVI rulebook. |
| [Android teaching/AI discussion and comments](https://www.reddit.com/r/androiddev/comments/1vr35pn/advice_for_my_android_students_about_using_ai/), displayed age two months | Participants disagree about AI use, but concrete examples stress explaining and reviewing produced code. | Keep human decisions/review and explain tradeoffs; do not equate generated volume with engineering understanding. |

The debugging threshold and per-checkout build serialization are project decisions derived from this session, not measured productivity claims from these sources. No percentage speedup is claimed.

## Memory, skills and KMP/CMP outcomes

Current rules belong in AGENTS/Human/guides; active status/evidence belongs in the handoff; superseded evidence belongs in history; private work ordering belongs in ignored Notes. New chats can recover project context from a clone without depending on this conversation or personal memory. User instructions remain authoritative when an installed skill or historical note disagrees.

KMP shares code/logic across targets; CMP supplies shared Compose UI on that foundation. Our workflow checks both reusable code and native integration, distinguishes host tests from native runtime acceptance, and preserves the configured targets/convention plugins. Resource packaging, entry ownership, storage migration and minimum-OS warnings remain explicit review concerns.

No additional skill pack, memory service, agent framework, desktop target or model switch is introduced. Official Kotlin skills, IDE inspections/MCP and library lookup remain options if repeated API/navigation gaps justify their setup. The existing repository instructions and scripts cover the demonstrated needs now.

## Follow-up and review

- **Applied now:** current/history separation, evidence-led debugging, sequential build ownership, change-based verification, corrected shared-artifact guidance, selective skill use and a fast document checker wired into existing CI. Human.md feedback is preserved.
- **Deferred:** expanded unit/UI coverage and minimum-iOS/native acceptance review remain post-feature work. A license, backend provisioning, remote CI validation, signing and distribution are not silently decided here.
- **Evaluate on the next few features:** did a fresh chat find current scope without reading history; did failures yield diagnostics before repeated edits; did final gates run once after relevant changes; were user edits preserved? Record a new rule only when a repeated, consequential gap justifies it. Do not create a separate metrics program or new tests for this retrospective.

Verification: the new document checker passes from the repository and `/private/tmp` (11 documents, 58 local link targets); shell syntax, workflow YAML/embedded shell blocks and diff whitespace checks pass. The user committed the prior membership change as `14cfd31` during this review; current app source matches HEAD and was left intact. App checks were not rerun for this documentation/tooling-only change. Remote CI remains unverified.

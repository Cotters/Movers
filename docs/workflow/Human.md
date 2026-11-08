# How we work

People decide behavior and review changes; AI helps implement them. For this portfolio project, keep backends replaceable and follow [public-source guidance](open-source.md).

1. **Understand first.** Read the relevant code, clarify requirements, and agree on a feature plan before implementation.
2. **Follow the app.** Reuse its architecture and conventions. Check current official Android documentation and use relevant skills.
3. **Change in small steps.** Keep work focused and reviewable. Separate feature work from unrelated refactors and upgrades.
4. **Verify honestly.** Run `./scripts/verify.sh` for host tests, lint, and a debug build. You test Android behavior on a physical device; agents never use emulators or simulators. Passing host checks does not prove the UI works.
5. **Leave a useful handoff.** Record decisions, evidence, unresolved questions, and the next action so a new chat can continue accurately.

This workflow reduces guesswork, catches regressions, and keeps project knowledge available beyond any single conversation. Details live in [development](development.md), [testing](testing.md), and [handoff](handoff.md).

## Issues and proposed improvements

Add a short observation and suggested change here. Discuss and agree on changes before promoting them into the workflow. Agents preserve this section and review it when updating the workflow.

- No open proposals yet.

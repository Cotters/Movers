# How we work

People decide behavior and review changes; AI helps implement them. For this portfolio project, keep backends replaceable and follow [public-source guidance](open-source.md).

1. **Understand first.** Read the relevant code, clarify requirements, and agree on a feature plan before implementation.
2. **Follow the app.** Reuse its shared Android/iOS architecture and [build conventions](../../build-logic/README.md). Keep platform integrations small. Check official documentation and use relevant skills.
3. **Change in small steps.** Keep work focused and reviewable. Separate feature work from unrelated refactors and upgrades.
4. **Verify honestly.** Run `./scripts/verify.sh` and both native smoke tests after each change set; extend coverage with features. Automated smoke tests on already-running targets are authorized. Manual interaction or starting/resetting devices needs a separate request. For shared/iOS changes, also run `./scripts/verify-ios.sh`. Physical-device acceptance remains yours.
5. **Leave a useful handoff.** Record decisions, evidence, unresolved questions, and the next action so a new chat can continue accurately.

This workflow reduces guesswork, catches regressions, and keeps project knowledge available beyond any single conversation. Details live in [development](development.md), [testing](testing.md), and [handoff](handoff.md).

## Issues and proposed improvements

Add a short observation and suggested change here. Discuss and agree on changes before promoting them into the workflow. Agents preserve this section and review it when updating the workflow.

- No open proposals yet.

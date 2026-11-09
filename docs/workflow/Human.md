# How we work

People decide behavior and review changes; AI helps implement them. For this portfolio project, keep backends replaceable and follow [public-source guidance](open-source.md).

1. **Understand first.** For substantial work, read the [handoff](handoff.md) and relevant code. Agree on consequential feature decisions; clear follow-up requests can proceed.
2. **Follow the app.** Reuse its shared Android/iOS architecture and [build conventions](../../build-logic/README.md). Keep platform integrations small. Check official documentation and use relevant skills.
3. **Use evidence.** Make focused changes. When a fix fails, inspect the actual failure before trying more changes. Keep builds for one checkout sequential.
4. **Verify proportionately.** App changes run `./scripts/verify.sh`; shared/iOS changes also run `./scripts/verify-ios.sh`. Docs-only changes run `./scripts/verify-docs.sh`. Update existing expectations; defer new tests to post-feature review. Agents never use emulators/simulators. People verify on physical devices.
5. **Leave concise memory.** Update current decisions, verification limits, and next action in the handoff; archive superseded history. New chats use repository files, not assumed personal memory.

This workflow reduces guesswork, catches regressions, and keeps project knowledge available beyond any single conversation. Details live in [development](development.md), [testing](testing.md), and [handoff](handoff.md).

## Issues and proposed improvements

Add a short observation and suggested change here. Discuss and agree on changes before promoting them into the workflow. Agents preserve this section and review it when updating the workflow.

- No open proposals yet.

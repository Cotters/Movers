# How we work

People decide behavior and review changes; AI helps implement them. For this portfolio project, keep backends replaceable and follow [public-source guidance](open-source.md).

1. **Understand first.** For substantial work, read the [handoff](handoff.md) and relevant code. Agree on consequential feature decisions; clear follow-up requests can proceed.
2. **Follow the app.** Reuse its shared Android/iOS architecture and [build conventions](../../build-logic/README.md). Keep platform integrations small. Check official documentation and use relevant skills. For UI, [agree unclear placements](../design/ui-guidelines.md) and raise layout tradeoffs before coding.
3. **Review before every code commit.** [Format, inspect diagnostics, run code checks and Kotlin lint, then review structure and SOLID](code-review.md). CI enforces Kotlin source lint. Fix code warnings; repeat affected checks after fixes and inspect staged content. Preserve user edits; report failing/unavailable checks. Diagnose failed fixes before trying again. Keep builds sequential.
4. **Verify proportionately.** Use static checks while iterating. Run builds and compilation-backed tests only when requested or needed for a substantial change/release; explain the need first. No automatic final builds. Verification commands live in [testing](testing.md); docs checks use `./scripts/verify-docs.sh`. Update existing expectations; defer new tests to post-feature review. Agents never use emulators/simulators. People verify on physical devices.
5. **Leave concise memory.** Update current decisions, verification limits, and next action in the handoff; archive superseded history. New chats use repository files, not assumed personal memory.

This workflow reduces guesswork, catches regressions, and keeps project knowledge available beyond any single conversation. Details live in [development](development.md), [testing](testing.md), and [handoff](handoff.md).

## Issues and proposed improvements

Add a short observation and suggested change here. Discuss and agree on changes before promoting them into the workflow. Agents preserve this section and review it when updating the workflow.

- No open proposals yet.

# Movers components

Wrappers, tokens and stroke icons are original project code. Visual references: [RikkaUI](https://github.com/rainxchzed/RikkaUi), [shadcn-multiplatform](https://github.com/jershell/shadcn-multiplatform), Sequel and Linear. No component source or visual assets were copied from those projects.

- **Compose Unstyled 2.10.2:** selected Maven Central component modules; MIT. Exact APIs were checked against the published sources. The [MIT notice](src/commonMain/composeResources/files/licenses/compose-unstyled-mit.txt) is bundled as a shared resource. Source: [Composable Horizons](https://github.com/composablehorizons/compose-unstyled).
- **Inter v4.1:** official release's Regular, Medium and SemiBold TTFs; SIL OFL 1.1. The [font licence](src/commonMain/composeResources/files/licenses/inter-ofl.txt) is bundled alongside the fonts. Source: [rsms/inter v4.1](https://github.com/rsms/inter/releases/tag/v4.1). Raw fonts total 1,248,684 bytes; this is not a packaged-size measurement.

The existing Compose convention supplies Material infrastructure. Movers controls use Foundation/Unstyled; an explicit theme bridge maps retained Material text, icon, scaffold, progress and snackbar-host roles to Movers tokens. The same production components appear in the development gallery, whose styling overrides stay local. Haze is not installed.

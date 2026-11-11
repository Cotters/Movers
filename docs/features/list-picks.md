# Random list picks

Implemented 11 November 2026. Every personal/shared list offers **Pick a film** in its normal menu; edit mode still shows only layout and Done. Empty/unavailable lists disable the action.

The entry ViewModel selects from its complete, currently visible membership projection, independent of the title filter. Pending Undo, committing and actively removing films are excluded. The existing movie detail destination opens with the list context; selecting never removes a film or writes activity.

The shared pure selection function weights each valid positive TMDB score linearly, clamped to 1–10. Missing/nonfinite scores and films without votes get weight 5. Every film retains a positive chance; lists without ratings use uniform selection. Cached values suffice, with no network request. Repeated picks can select the same film.

Research: Kotlin's [bounded random doubles](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.random/-random/next-double.html), living documentation checked 11 November 2026. The weights are a product choice, not a claimed recommendation algorithm.

Source/diff/docs checks only. No new tests or builds; physical Android/iPhone acceptance and expanded selection coverage remain pending.

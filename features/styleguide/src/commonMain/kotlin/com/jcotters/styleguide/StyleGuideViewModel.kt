package com.jcotters.styleguide

import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModel
import com.jcotters.designsystem.ComponentDensity
import com.jcotters.designsystem.MoversStyleSettings
import com.jcotters.designsystem.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class GuideSection { Basics, Controls, Overlays, Films }

internal data class StyleGuideState(
    val settings: MoversStyleSettings = MoversStyleSettings(),
    val section: GuideSection = GuideSection.Basics,
    val resetVersion: Int = 0,
)

/** Entry-owned, session-only preview choices. No application preferences or data access. */
internal class StyleGuideViewModel : ViewModel() {
    private val state = MutableStateFlow(StyleGuideState())
    val uiState = state.asStateFlow()

    fun theme(value: ThemeMode) { state.update { it.copy(settings = it.settings.copy(theme = value)) } }
    fun corners(value: Dp) { state.update { it.copy(settings = it.settings.copy(cornerRadius = value)) } }
    fun density(value: ComponentDensity) { state.update { it.copy(settings = it.settings.copy(density = value)) } }
    fun section(value: GuideSection) { state.update { it.copy(section = value) } }
    fun reset() { state.update { it.copy(settings = MoversStyleSettings(), resetVersion = it.resetVersion + 1) } }
}

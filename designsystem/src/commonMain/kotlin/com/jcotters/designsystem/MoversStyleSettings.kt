package com.jcotters.designsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class PreviewTheme { System, Light, Dark }
enum class ComponentDensity { Compact, Comfortable }

/** Experimental settings used only by the development gallery. */
data class MoversStyleSettings(
    val theme: PreviewTheme = PreviewTheme.System,
    val cornerRadius: Dp = 12.dp,
    val density: ComponentDensity = ComponentDensity.Comfortable,
)

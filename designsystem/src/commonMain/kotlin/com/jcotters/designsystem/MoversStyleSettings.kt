package com.jcotters.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ThemeMode { System, Light, Dark }
enum class ComponentDensity { Compact, Comfortable }

/** Shared production defaults; the development gallery can override them locally. */
@Immutable
data class MoversStyleSettings(
    val theme: ThemeMode = ThemeMode.System,
    val cornerRadius: Dp = 16.dp,
    val imageCornerRadius: Dp = 8.dp,
    val density: ComponentDensity = ComponentDensity.Comfortable,
)

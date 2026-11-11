package com.jcotters.movers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalDevelopmentToolsEnabled = staticCompositionLocalOf { false }

@Composable
internal expect fun developmentToolsEnabled(): Boolean

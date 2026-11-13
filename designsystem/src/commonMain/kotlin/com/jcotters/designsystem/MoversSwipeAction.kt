package com.jcotters.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class MoversSwipeAction(
    val label: String,
    val accessibilityLabel: String,
    val onClick: () -> Unit,
    val icon: @Composable (Color) -> Unit,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
    val allowsFullSwipe: Boolean = false,
) {
    internal val canFullSwipe: Boolean get() = allowsFullSwipe && enabled && !destructive
}

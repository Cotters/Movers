package com.jcotters.designsystem

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.composeunstyled.DropdownMenuPanel
import com.composeunstyled.UnstyledDropdownMenu
import com.composeunstyled.UnstyledDropdownMenuItem

@Composable
fun MoversDropdownMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    anchor: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tokens = MoversDesign.tokens
    val shape = RoundedCornerShape(tokens.cornerRadius + 4.dp)
    val density = LocalDensity.current
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val insets = WindowInsets.safeDrawing
    val availableHeight = with(density) {
        (windowHeight - insets.getTop(density) - insets.getBottom(density)).toDp()
    }
    // Anchored content is measured unbounded by Unstyled; bound the viewport before scrolling.
    val maximumHeight = (availableHeight - 32.dp).coerceAtLeast(48.dp)
    UnstyledDropdownMenu(expanded, onExpandedChange, modifier, sideOffset = 6.dp, anchor = anchor,
        panel = {
            DropdownMenuPanel(
                modifier = Modifier.widthIn(min = 220.dp, max = 320.dp).heightIn(max = maximumHeight).shadow(12.dp, shape).clip(shape)
                    .background(tokens.colors.surface).border(1.dp, tokens.colors.border, shape)
                    .verticalScroll(rememberScrollState()).padding(6.dp),
                enter = fadeIn(tween(160)), exit = fadeOut(tween(120)),
            ) { content() }
        },
    )
}

@Composable
fun MoversDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val tokens = MoversDesign.tokens
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(tokens.cornerRadius)
    UnstyledDropdownMenuItem(onClick = onClick, enabled = enabled, interactionSource = interaction,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape)
            .background(if (focused || pressed) tokens.colors.raised else tokens.colors.surface)
            .border(1.dp, if (focused) tokens.colors.accent else tokens.colors.surface, shape)
            .padding(horizontal = 10.dp, vertical = tokens.spacing)) {
        CompositionLocalProvider(LocalContentColor provides when {
            !enabled -> tokens.colors.muted
            destructive -> tokens.colors.danger
            else -> tokens.colors.content
        }) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                leadingIcon?.invoke()
                ProvideTextStyle(tokens.type.label, content = text)
            }
        }
    }
}

package com.jcotters.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.composeunstyled.UnstyledButton

enum class ButtonVariant { Primary, Secondary, Quiet, Destructive }

@Composable
fun MoversButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    MoversButton(onClick, modifier, variant, enabled, loading) {
        MoversText(
            if (loading) "$label…" else label,
            style = MoversDesign.tokens.type.label,
            color = LocalContentColor.current
        )
    }
}

/** Slot overload keeps feature-owned labels/icons out of the design system. */
@Composable
fun MoversButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    contentColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = MoversDesign.tokens
    val colors = tokens.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(tokens.cornerRadius)
    val fill = when {
        !enabled -> colors.raised
        pressed -> colors.border
        variant == ButtonVariant.Primary -> colors.accent
        variant == ButtonVariant.Destructive -> colors.dangerSurface
        variant == ButtonVariant.Secondary -> colors.raised
        else -> Color.Transparent
    }
    val background by animateColorAsState(fill, label = "Button surface")
    val foreground = if (contentColor != Color.Unspecified) contentColor else when {
        !enabled -> colors.muted
        pressed -> colors.content
        variant == ButtonVariant.Primary -> colors.onAccent
        variant == ButtonVariant.Destructive -> colors.danger
        else -> colors.content
    }
    UnstyledButton(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = interaction,
        contentPadding = PaddingValues(horizontal = tokens.controlPadding, vertical = tokens.spacing * 1.25f),
        modifier = modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).clip(shape).background(background)
            .border(if (focused) 2.dp else 1.dp, if (focused) colors.accent else Color.Transparent, shape)
            .semantics { if (loading) stateDescription = "Loading" },
    ) {
        CompositionLocalProvider(LocalContentColor provides foreground) {
            ProvideTextStyle(tokens.type.label) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}

@Composable
fun MoversTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    contentColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit,
) {
    MoversButton(
        onClick, modifier, variant = if (destructive) ButtonVariant.Destructive else ButtonVariant.Quiet,
        enabled = enabled, contentColor = contentColor, content = content
    )
}

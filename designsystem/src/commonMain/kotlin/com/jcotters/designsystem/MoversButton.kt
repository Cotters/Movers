package com.jcotters.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
    val foreground = when {
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
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing), verticalAlignment = Alignment.CenterVertically) {
            MoversText(if (loading) "$label…" else label, style = tokens.type.label, color = foreground)
        }
    }
}

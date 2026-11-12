package com.jcotters.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composeunstyled.UnstyledButton

@Composable
fun MoversAvatar(
    name: String,
    modifier: Modifier = Modifier,
    background: Color = MoversDesign.tokens.colors.accent,
    contentColor: Color = MoversDesign.tokens.colors.onAccent,
) {
    AvatarInitial(name, background, contentColor, modifier.clearAndSetSemantics { contentDescription = name })
}

/** A 32 dp avatar inside a touch target that can grow with larger text. */
@Composable
fun MoversAvatarButton(
    name: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = MoversDesign.tokens.colors.accent,
    contentColor: Color = MoversDesign.tokens.colors.onAccent,
) {
    val tokens = MoversDesign.tokens
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    UnstyledButton(
        onClick = onClick,
        interactionSource = interaction,
        contentPadding = PaddingValues(8.dp),
        modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).clip(CircleShape)
            .background(if (pressed) tokens.colors.raised else Color.Transparent)
            .border(2.dp, if (focused) tokens.colors.accent else Color.Transparent, CircleShape)
            .semantics { contentDescription = label },
    ) {
        AvatarInitial(name, background, contentColor, Modifier.clearAndSetSemantics {})
    }
}

@Composable
private fun AvatarInitial(name: String, background: Color, contentColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.size(32.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)).background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        MoversText(
            name.trim().take(1).uppercase().ifEmpty { "?" },
            style = MoversDesign.tokens.type.label,
            color = contentColor,
        )
    }
}

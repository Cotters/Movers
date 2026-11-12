package com.jcotters.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp

/** Controlled input: the caller owns text/validation; focus, cursor and IME stay in Compose. */
@Composable
fun MoversTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val tokens = MoversDesign.tokens
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(tokens.cornerRadius)
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val actions = keyboardActions ?: KeyboardActions(onDone = { focus.clearFocus(); keyboard?.hide() })
    val foreground = if (enabled) tokens.colors.content else tokens.colors.muted
    val hasIcons = leadingIcon != null || trailingIcon != null
    CompositionLocalProvider(
        LocalContentColor provides foreground,
        LocalTextSelectionColors provides TextSelectionColors(tokens.colors.accent, tokens.colors.accent.copy(alpha = 0.25f)),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = tokens.type.body.copy(color = foreground),
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = actions,
            interactionSource = interaction,
            cursorBrush = SolidColor(tokens.colors.accent),
            modifier = modifier.fillMaxWidth(),
            decorationBox = { input ->
                Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing)) {
                    if (label != null) ProvideTextStyle(tokens.type.label, content = label)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape).background(tokens.colors.surface)
                            .border(if (focused) 2.dp else 1.dp, if (focused) tokens.colors.accent else tokens.colors.controlOutline, shape)
                            .padding(
                                horizontal = if (hasIcons) tokens.spacing * 1.5f else tokens.controlPadding,
                                vertical = if (hasIcons) 0.dp else 12.dp,
                            ),
                        horizontalArrangement = Arrangement.spacedBy(tokens.spacing),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        leadingIcon?.invoke()
                        Box(Modifier.weight(1f)) {
                            if (value.isEmpty() && placeholder != null) {
                                CompositionLocalProvider(LocalContentColor provides tokens.colors.muted, content = placeholder)
                            }
                            input()
                        }
                        trailingIcon?.invoke()
                    }
                }
            },
        )
    }
}

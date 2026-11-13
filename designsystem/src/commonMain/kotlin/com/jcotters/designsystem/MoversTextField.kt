package com.jcotters.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.error as semanticsError
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.composeunstyled.TextInput
import com.composeunstyled.UnstyledTextField

@Composable
fun MoversTextField(
    state: TextFieldState,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    error: String? = null,
    multiline: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    onSubmit: (() -> Unit)? = null,
) {
    val tokens = MoversDesign.tokens
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val shape = RoundedCornerShape(tokens.cornerRadius)
    val outline = when {
        error != null -> tokens.colors.danger
        focused -> tokens.colors.accent
        else -> tokens.colors.controlOutline
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(tokens.spacing)) {
        MoversText(label, style = tokens.type.label)
        UnstyledTextField(
            state = state,
            enabled = enabled,
            accessibilityLabel = label,
            interactionSource = interaction,
            textStyle = tokens.type.body,
            textColor = if (enabled) tokens.colors.content else tokens.colors.muted,
            cursorBrush = SolidColor(tokens.colors.accent),
            selectionColors = TextSelectionColors(tokens.colors.accent, tokens.colors.accent.copy(alpha = 0.25f)),
            lineLimits = if (multiline) TextFieldLineLimits.MultiLine(
                minHeightInLines = 3,
                maxHeightInLines = 6
            ) else TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            onKeyboardAction = { focus.clearFocus(); keyboard?.hide(); onSubmit?.invoke() },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape).background(tokens.colors.surface)
                .border(if (focused) 2.dp else 1.dp, outline, shape)
                .semantics { error?.let { semanticsError(it) } },
        ) {
            TextInput(
                Modifier.fillMaxWidth().padding(horizontal = tokens.controlPadding, vertical = 12.dp),
                placeholder = { MoversText(placeholder, color = tokens.colors.muted) },
            )
        }
        error?.let { MoversText(it, style = tokens.type.caption, color = tokens.colors.danger) }
    }
}

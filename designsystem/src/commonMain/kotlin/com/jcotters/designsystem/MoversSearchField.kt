package com.jcotters.designsystem

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction

/** Inline hint and unpadded action targets keep search to a single, accessible control row. */
@Composable
fun MoversSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSearch: (() -> Unit)? = null,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    MoversTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.semantics { contentDescription = label },
        placeholder = { MoversText(label, color = MoversDesign.tokens.colors.muted) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); onSearch?.invoke() }),
        leadingIcon = if (onSearch == null) { { MoversIcon(MoversSymbol.Search) } } else null,
        trailingIcon = {
            Row {
                if (value.isNotEmpty()) {
                    MoversIconButton(MoversSymbol.Close, "Clear search", { onValueChange("") }, enabled = enabled)
                }
                if (onSearch != null) {
                    MoversIconButton(MoversSymbol.Search, "Submit search", onSearch, enabled = enabled && value.isNotBlank())
                }
            }
        },
    )
}

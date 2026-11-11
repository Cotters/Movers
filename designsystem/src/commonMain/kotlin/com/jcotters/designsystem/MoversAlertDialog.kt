package com.jcotters.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Visibility belongs to the feature; this component owns only modal mechanics and layout. */
@Composable
fun MoversAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
) {
    val tokens = MoversDesign.tokens
    MoversDialogSurface(visible = true, onDismiss = onDismissRequest, modifier = modifier) {
        ProvideTextStyle(tokens.type.title, content = title)
        ProvideTextStyle(tokens.type.body, content = text)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            dismissButton?.invoke()
            confirmButton()
        }
    }
}

package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.jcotters.contract.watchlists.domain.Watchlist
import com.jcotters.designsystem.MoversAlertDialog
import com.jcotters.designsystem.MoversTextButton
import com.jcotters.designsystem.MoversTextField

@Composable
internal fun ListNameFields(
    name: String,
    onNameChange: (String) -> Unit,
    lists: List<Watchlist>,
    excludedId: String? = null,
    error: String? = null,
    enabled: Boolean = true,
) {
    val focus = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MoversTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("List name") },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        )
        if (name.isNotBlank() && lists.any { it.id != excludedId && it.name.equals(name.trim(), ignoreCase = true) }) {
            Text(
                "A list with this name already exists. You can still use this name.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
internal fun ListNameDialog(
    title: String,
    lists: List<Watchlist>,
    initialName: String = "",
    excludedId: String? = null,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    MoversAlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = { ListNameFields(name, { name = it }, lists, excludedId, error, !busy) },
        confirmButton = {
            MoversTextButton(onClick = { onSave(name) }, enabled = !busy && name.isNotBlank()) {
                Text(if (excludedId == null) "Create" else "Save")
            }
        },
        dismissButton = { MoversTextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}

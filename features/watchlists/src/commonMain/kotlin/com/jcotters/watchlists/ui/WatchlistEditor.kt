package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.jcotters.contract.watchlists.domain.Watchlist

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
        OutlinedTextField(
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
            Text("A list with this name already exists. You can still use this name.", style = MaterialTheme.typography.bodySmall)
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
    // Bound the text form independently of native dialog intrinsic-width measurement.
    AlertDialog(
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(0.9f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = { ListNameFields(name, { name = it }, lists, excludedId, error, !busy) },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = !busy && name.isNotBlank()) {
                Text(if (excludedId == null) "Create" else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}

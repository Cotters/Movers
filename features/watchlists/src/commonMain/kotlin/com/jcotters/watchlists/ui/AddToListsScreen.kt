package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.navigation.Navigator
import com.jcotters.watchlists.AddToListsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun AddToListsRoute(movieId: Int, initialListId: String?, navigator: Navigator) {
    val viewModel: AddToListsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(movieId, initialListId) { viewModel.load(movieId, initialListId) }
    LaunchedEffect(state.completed) { if (state.completed) navigator.goBack() }
    LaunchedEffect(state.createdId) {
        if (state.createdId != null) { creating = false; name = ""; viewModel.consumeCreated() }
    }
    Column(Modifier.fillMaxWidth().heightIn(max = 680.dp).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Add to list", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = navigator::goBack, enabled = !state.busy) { Text("Close") }
        }
        Text(state.title, style = MaterialTheme.typography.titleMedium)
        if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!state.loading && state.error == null && state.lists.isEmpty()) Text("Create your first list to save this movie.")
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { viewModel.clearError(); viewModel.load(movieId, initialListId) }) { Text("Retry") }
        }
        LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(max = 340.dp)) {
            items(state.lists, key = { it.id }) { list ->
                val existing = list.id in state.existing
                Row(
                    Modifier.fillMaxWidth().toggleable(
                        value = existing || list.id in state.selected,
                        enabled = !existing && !state.busy,
                        role = Role.Checkbox,
                        onValueChange = { viewModel.toggle(list.id) },
                    ).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = existing || list.id in state.selected, onCheckedChange = null, enabled = !existing && !state.busy)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(list.name)
                        Text(if (existing) "Already added" else "${list.movieCount} ${if (list.movieCount == 1) "movie" else "movies"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (creating) {
            ListNameFields(name, { name = it }, state.lists, error = state.error, enabled = !state.busy)
            Row {
                TextButton(onClick = { viewModel.create(name) }, enabled = !state.busy && name.isNotBlank()) { Text("Create") }
                TextButton(onClick = { creating = false; viewModel.clearError() }, enabled = !state.busy) { Text("Cancel") }
            }
        } else {
            TextButton(onClick = { viewModel.clearError(); creating = true }, enabled = !state.busy) { Text("Create new list") }
        }
        Button(onClick = viewModel::add, enabled = !state.loading && !state.busy && state.destinations.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text("Add to ${state.destinations.size} ${if (state.destinations.size == 1) "list" else "lists"}")
        }
    }
}

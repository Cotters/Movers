package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
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
    val scroll = rememberLazyListState()
    LaunchedEffect(movieId) { viewModel.load(movieId) }
    LaunchedEffect(state.loading, initialListId) {
        if (!state.loading && initialListId != null) {
            val index = state.lists.indexOfFirst { it.id == initialListId }
            if (index >= 0) scroll.scrollToItem(index)
        }
    }
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
            TextButton(onClick = {
                viewModel.clearError()
                if (state.readFailed) viewModel.load(movieId)
            }) { Text(if (state.readFailed) "Retry" else "Dismiss") }
        }
        LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(max = 340.dp), state = scroll) {
            items(state.lists, key = { it.id }) { list ->
                val existing = list.id in state.existing
                val enabled = !state.loading && !state.readFailed && !state.creating && list.id !in state.saving
                Row(
                    Modifier.fillMaxWidth().toggleable(
                        value = existing,
                        enabled = enabled,
                        role = Role.Checkbox,
                        onValueChange = { viewModel.setMembership(list.id, it) },
                    ).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = existing, onCheckedChange = null, enabled = enabled)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(list.name)
                        list.shared?.let { shared -> Text("Shared · ${shared.members.size} members",style = MaterialTheme.typography.bodySmall) }
                        Text(if (list.id in state.saving) "Saving…" else "${list.movieCount} ${if (list.movieCount == 1) "movie" else "movies"}", style = MaterialTheme.typography.bodySmall)
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
            TextButton(onClick = { viewModel.clearError(); creating = true }, enabled = !state.busy && !state.loading && !state.readFailed) { Text("Create new list") }
        }
    }
}

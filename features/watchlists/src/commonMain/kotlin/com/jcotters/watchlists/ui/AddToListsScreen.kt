package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.designsystem.MoversCheckboxRow
import com.jcotters.designsystem.MoversTextButton
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
    Column(Modifier.fillMaxWidth().heightIn(max = 680.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Add to list", style = MaterialTheme.typography.headlineSmall)
            MoversTextButton(onClick = navigator::goBack, enabled = !state.busy) { Text("Close") }
        }
        Text(state.title, style = MaterialTheme.typography.titleMedium)
        if (!state.loading && state.error == null && state.lists.isEmpty()) Text("Create your first list to save this movie.")
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            MoversTextButton(onClick = {
                viewModel.clearError()
                if (state.readFailed) viewModel.load(movieId)
            }) { Text(if (state.readFailed) "Retry" else "Dismiss") }
        }
        LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(max = 340.dp), state = scroll) {
            items(state.lists, key = { it.id }) { list ->
                val existing = list.id in state.existing
                val enabled = !state.loading && !state.readFailed && !state.creating && list.id !in state.saving
                val count = "${list.movieCount} ${if (list.movieCount == 1) "movie" else "movies"}"
                val shared = list.shared?.let { " · ${it.members.size} members" }.orEmpty()
                MoversCheckboxRow(
                    label = list.name,
                    checked = existing,
                    onCheckedChange = { viewModel.setMembership(list.id, it) },
                    enabled = enabled,
                    description = if (list.id in state.saving) "Saving…" else count + shared,
                )
            }
        }
        if (creating) {
            ListNameFields(name, { name = it }, state.lists, error = state.error, enabled = !state.busy)
            Row {
                MoversTextButton(onClick = { viewModel.create(name) }, enabled = !state.busy && name.isNotBlank()) { Text("Create") }
                MoversTextButton(onClick = { creating = false; viewModel.clearError() }, enabled = !state.busy) { Text("Cancel") }
            }
        } else {
            MoversTextButton(onClick = { viewModel.clearError(); creating = true }, enabled = !state.busy && !state.loading && !state.readFailed) { Text("Create new list") }
        }
    }
}

package com.jcotters.watchlists.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.designsystem.MoversButton
import com.jcotters.designsystem.MoversListItem
import com.jcotters.designsystem.MoversTextButton
import com.jcotters.designsystem.MoversTopBar
import com.jcotters.navigation.Navigator
import com.jcotters.watchlists.WatchlistsViewModel
import com.jcotters.watchlists.navigation.WatchlistDetailKey
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WatchlistsRoute(navigator: Navigator) {
    val viewModel: WatchlistsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.createdId) {
        state.createdId?.let { id ->
            creating = false
            viewModel.consumeCreated()
            navigator.navigate(WatchlistDetailKey(id))
        }
    }
    Scaffold(topBar = {
        MoversTopBar(title = { Text("Watchlists") }, actions = {
            MoversTextButton(onClick = { navigator.navigate(com.jcotters.watchlists.navigation.JoinWatchlistKey) }) { Text("Join list") }
            MoversTextButton(onClick = { viewModel.clearError(); creating = true }, enabled = !state.loading && !state.busy) { Text("Create list") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) LinearProgressIndicator()
            state.syncError?.let {
                Text("Shared updates unavailable. Cached lists remain readable.",Modifier.padding(16.dp))
                MoversTextButton(onClick = viewModel::refreshShared) { Text("Retry shared updates") }
            }
            state.error?.let {
                Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                MoversTextButton(onClick = { viewModel.clearError(); viewModel.retry() }) { Text("Retry") }
            }
            if (!state.loading && state.error == null && state.lists.isEmpty()) {
                Column(Modifier.padding(24.dp)) {
                    Text("Create a list for your next movie night.")
                    MoversButton(onClick = { viewModel.clearError(); creating = true }) { Text("Create your first list") }
                }
            }
            LazyColumn {
                items(state.lists, key = { it.id }) { list ->
                    MoversListItem(
                        headlineContent = { Text(list.name) },
                        supportingContent = { Text("${list.movieCount} ${if (list.movieCount == 1) "movie" else "movies"}" + (list.shared?.let { " · Shared · ${it.members.size} members" } ?: "")) },
                        leadingContent = list.shared?.let { info -> { com.jcotters.presentation.ParticipantAvatar(info.creator.name,info.creator.color) } },
                        modifier = Modifier.clickable { navigator.navigate(WatchlistDetailKey(list.id)) },
                    )
                }
            }
        }
    }
    if (creating) ListNameDialog("Create list", state.lists, busy = state.busy, error = state.error,
        onDismiss = { creating = false; viewModel.clearError() }, onSave = viewModel::create)
}

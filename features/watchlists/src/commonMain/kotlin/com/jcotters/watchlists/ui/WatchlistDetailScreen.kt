package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.details.navigation.MovieDetailsKey
import com.jcotters.navigation.Navigator
import com.jcotters.presentation.MovieListRow
import com.jcotters.presentation.moviePosterTransitionModifier
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.add
import com.jcotters.presentation.resources.arrow_back
import com.jcotters.presentation.resources.more
import com.jcotters.search.navigation.MovieSearchKey
import com.jcotters.watchlists.WatchlistDetailViewModel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WatchlistDetailRoute(listId: String, navigator: Navigator, stackId: String) {
    val viewModel: WatchlistDetailViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menu by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(listId) { viewModel.load(listId) }
    LaunchedEffect(state.deleted) { if (state.deleted) navigator.goBack() }
    LaunchedEffect(state.renamed) { if (state.renamed) { renaming = false; viewModel.consumeRenamed() } }
    val list = state.content?.watchlist
    Scaffold(topBar = {
        TopAppBar(title = { Text(list?.name ?: "Watchlist") }, navigationIcon = {
            IconButton(onClick = navigator::goBack) { Icon(painterResource(Res.drawable.arrow_back), "Back") }
        }, actions = {
            IconButton(onClick = { navigator.navigate(MovieSearchKey(listId)) }, enabled = list != null && !state.busy) {
                Icon(painterResource(Res.drawable.add), "Add movies")
            }
            IconButton(onClick = { menu = true }, enabled = list != null && !state.busy) {
                Icon(painterResource(Res.drawable.more), "List options")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Rename list") }, onClick = { menu = false; viewModel.clearError(); renaming = true })
                DropdownMenuItem(text = { Text("Delete list") }, onClick = { menu = false; viewModel.clearError(); deleting = true })
            }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading || state.busy) LinearProgressIndicator()
            state.error?.let {
                Text(it, Modifier.padding(16.dp))
                TextButton(onClick = { viewModel.clearError(); viewModel.load(listId) }) { Text("Retry") }
            }
            if (!state.loading && state.error == null && state.content == null) Text("This list no longer exists.", Modifier.padding(24.dp))
            state.content?.let { content ->
                Text("${content.movies.size} ${if (content.movies.size == 1) "movie" else "movies"}", Modifier.padding(16.dp))
                if (content.movies.isEmpty()) Text("No movies yet. Tap + to find films.", Modifier.padding(24.dp))
                LazyColumn {
                    items(content.movies, key = { it.id }) { movie ->
                        MovieListRow(movie, Modifier.height(180.dp).fillMaxWidth(),
                            posterModifier = moviePosterTransitionModifier(movie.id, stackId),
                            onOpen = { navigator.navigate(MovieDetailsKey(movie.id, targetListId = listId)) }) {
                            TextButton(onClick = { viewModel.removeMovie(movie.id) }, enabled = !state.busy) { Text("Remove") }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
    if (renaming && list != null) ListNameDialog("Rename list", state.lists, list.name, list.id, state.busy, state.error,
        onDismiss = { renaming = false; viewModel.clearError() }, onSave = viewModel::rename)
    if (deleting && list != null) AlertDialog(
        onDismissRequest = { if (!state.busy) deleting = false }, title = { Text("Delete list?") },
        text = { Column { Text("Delete “${list.name}” and its movie selections?"); state.error?.let { Text(it) } } },
        confirmButton = { TextButton(onClick = viewModel::delete, enabled = !state.busy) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleting = false }, enabled = !state.busy) { Text("Cancel") } },
    )
}

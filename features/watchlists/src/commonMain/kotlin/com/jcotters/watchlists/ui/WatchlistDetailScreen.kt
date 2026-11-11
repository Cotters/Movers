package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.contract.watchlists.data.FilmContribution
import com.jcotters.contract.watchlists.domain.Watchlist
import com.jcotters.details.navigation.MovieDetailsKey
import com.jcotters.navigation.Navigator
import com.jcotters.presentation.MovieListRow
import com.jcotters.presentation.MovieMembershipButton
import com.jcotters.presentation.formatActivityTime
import com.jcotters.presentation.moviePosterTransitionModifier
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.add
import com.jcotters.presentation.resources.arrow_back
import com.jcotters.presentation.resources.more
import com.jcotters.search.navigation.MovieSearchKey
import com.jcotters.watchlists.WatchlistDetailViewModel
import com.jcotters.watchlists.navigation.SharedListKey
import com.jcotters.watchlists.navigation.SharedSection
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
    var editing by rememberSaveable(listId) { mutableStateOf(false) }
    var attribution by remember(listId) { mutableStateOf<FilmContribution?>(null) }
    var exportContent by remember(listId) { mutableStateOf<com.jcotters.contract.watchlists.domain.WatchlistContent?>(null) }
    LaunchedEffect(listId) { viewModel.load(listId) }
    LaunchedEffect(state.deleted) { if (state.deleted) navigator.goBack() }
    LaunchedEffect(state.renamed) { if (state.renamed) { renaming = false; viewModel.consumeRenamed() } }
    val list = state.content?.watchlist
    Scaffold(topBar = {
        TopAppBar(title = { Text(list?.name ?: "Watchlist") }, navigationIcon = {
            IconButton(onClick = navigator::goBack) { Icon(painterResource(Res.drawable.arrow_back), "Back") }
        }, actions = {
            if (editing) TextButton(onClick = { editing = false }, enabled = list != null && !state.busy) { Text("Done") }
            IconButton(onClick = { navigator.navigate(MovieSearchKey(listId)) }, enabled = list != null && !state.busy) {
                Icon(painterResource(Res.drawable.add), "Add movies")
            }
            IconButton(onClick = { menu = true }, enabled = list != null && !state.busy) {
                Icon(painterResource(Res.drawable.more), "List options")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Import films") }, onClick = { menu = false; navigator.navigate(com.jcotters.watchlists.navigation.FilmImportKey(listId)) })
                DropdownMenuItem(text = { Text("Export list") }, onClick = { menu = false; exportContent = state.content })
                DropdownMenuItem(text = { Text(if (editing) "Done editing" else "Edit list") }, onClick = { menu = false; editing = !editing })
                DropdownMenuItem(text = { Text("Rename list") }, onClick = { menu = false; viewModel.clearError(); renaming = true })
                DropdownMenuItem(text = { Text(if (list?.shared != null) "Share link" else "Share list") }, onClick = { menu = false; navigator.navigate(SharedListKey(listId,SharedSection.Sharing)) })
                if (list?.shared != null) {
                    DropdownMenuItem(text = { Text("Members") }, onClick = { menu = false; navigator.navigate(SharedListKey(listId,SharedSection.Members)) })
                    DropdownMenuItem(text = { Text("Activity") }, onClick = { menu = false; navigator.navigate(SharedListKey(listId,SharedSection.Activity)) })
                }
                HorizontalDivider()
                DropdownMenuItem(text = { Text(if (list?.shared != null) "Leave list" else "Delete list", color = MaterialTheme.colorScheme.error) },
                    onClick = { menu = false; viewModel.clearError(); deleting = true })
            }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading || state.busy) LinearProgressIndicator()
            state.error?.let {
                Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.clearError(); viewModel.load(listId) }) { Text("Retry") }
            }
            if (!state.loading && state.error == null && state.content == null) Text("This list no longer exists.", Modifier.padding(24.dp))
            state.content?.let { content ->
                WatchlistSummary(
                    movieCount = content.movies.size,
                    sharedInfo = content.watchlist.shared,
                    onMembers = { navigator.navigate(SharedListKey(listId, SharedSection.Members)) },
                )
                if (content.movies.isEmpty()) Text("No movies yet. Tap + to find films.", Modifier.padding(24.dp))
                LazyColumn(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    items(content.movies, key = { it.id }) { movie ->
                        MovieListRow(movie, Modifier.height(180.dp).fillMaxWidth(),
                            posterModifier = moviePosterTransitionModifier(movie.id, stackId),
                            onOpen = { navigator.navigate(MovieDetailsKey(movie.id, targetListId = listId)) },
                            posterOverlay = {
                                content.contributions[movie.id]?.let { contribution ->
                                    FilmContributorButton(
                                        contribution = contribution,
                                        onClick = { attribution = contribution },
                                        modifier = Modifier.align(Alignment.BottomEnd),
                                    )
                                }
                            },
                        ) {
                            if (editing) {
                                MovieMembershipButton(movieTitle = movie.title, isAdded = true,
                                    onClick = { viewModel.removeMovie(movie.id) }, enabled = !state.busy,
                                    saving = movie.id in state.removing)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
    exportContent?.let { ExportListSheet(it, onDismiss = { exportContent = null }) }
    if (renaming && list != null) ListNameDialog("Rename list", state.lists, list.name, list.id, state.busy, state.error,
        onDismiss = { renaming = false; viewModel.clearError() }, onSave = viewModel::rename)
    if (deleting && list != null) ListRemovalDialog(
        list = list,
        busy = state.busy,
        error = state.error,
        onDismiss = { deleting = false; viewModel.clearError() },
        onConfirm = { if (list.shared != null) viewModel.leave() else viewModel.delete() },
    )
    attribution?.let { contribution ->
        FilmAttributionDialog(contribution, onDismiss = { attribution = null })
    }
}

@Composable
private fun ListRemovalDialog(
    list: Watchlist,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val shared = list.shared != null
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (shared) "Leave list?" else "Delete list?") },
        text = {
            Column {
                Text(if (shared) "Remove “${list.name}” from this device? Other members keep access. If you are the last member, the list will be retired."
                    else "Delete “${list.name}” and its movie selections?")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(if (shared) "Leave" else "Delete")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}

@Composable
private fun FilmAttributionDialog(contribution: FilmContribution, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(contribution.actor.name) },
        text = { Text("${if (contribution.imported) "Imported" else "Added"} · ${formatActivityTime(contribution.addedAt)}") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

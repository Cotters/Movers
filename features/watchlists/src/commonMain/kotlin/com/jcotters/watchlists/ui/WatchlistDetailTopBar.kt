package com.jcotters.watchlists.ui

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import com.jcotters.designsystem.MoversDropdownMenu
import com.jcotters.designsystem.MoversDropdownMenuItem
import com.jcotters.designsystem.MoversIconButton
import com.jcotters.designsystem.MoversTextButton
import com.jcotters.designsystem.MoversTopBar
import com.jcotters.details.navigation.MovieDetailsKey
import com.jcotters.navigation.Navigator
import com.jcotters.presentation.MovieLayoutToggle
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.add
import com.jcotters.presentation.resources.arrow_back
import com.jcotters.presentation.resources.more
import com.jcotters.search.navigation.MovieSearchKey
import com.jcotters.watchlists.WatchlistDetailState
import com.jcotters.watchlists.WatchlistDetailViewModel
import com.jcotters.watchlists.navigation.ListTransferKey
import com.jcotters.watchlists.navigation.SharedListKey
import com.jcotters.watchlists.navigation.SharedSection
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun WatchlistDetailTopBar(
    listId: String,
    state: WatchlistDetailState,
    viewModel: WatchlistDetailViewModel,
    navigator: Navigator,
    editing: Boolean,
    showGrid: Boolean,
    onLayoutChange: () -> Unit,
    onEditingChanged: (Boolean) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val list = state.content?.watchlist
    MoversTopBar(
        title = { Text(list?.name ?: "Watchlist", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (!editing) {
                MoversIconButton(onClick = navigator::goBack) {
                    Icon(
                        painterResource(Res.drawable.arrow_back),
                        "Back"
                    )
                }
            }
        },
        actions = {
            MovieLayoutToggle(showGrid = showGrid, onClick = onLayoutChange)
            if (editing) {
                MoversTextButton(onClick = { onEditingChanged(false) }) { Text("Done") }
            } else {
                MoversIconButton(
                    onClick = { navigator.navigate(MovieSearchKey(listId)) },
                    enabled = list != null && !state.busy
                ) {
                    Icon(painterResource(Res.drawable.add), "Add movies")
                }
                WatchlistOptions(
                    state, viewModel, navigator, listId,
                    onEdit = { onEditingChanged(true) }, onRename = onRename, onDelete = onDelete
                )
            }
        })
}

@Composable
private fun WatchlistOptions(
    state: WatchlistDetailState,
    viewModel: WatchlistDetailViewModel,
    navigator: Navigator,
    listId: String,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val list = state.content?.watchlist
    var menu by rememberSaveable { mutableStateOf(false) }
    MoversDropdownMenu(expanded = menu, onExpandedChange = { menu = it }, anchor = {
        MoversIconButton(onClick = { menu = !menu }, enabled = list != null && !state.busy) {
            Icon(painterResource(Res.drawable.more), "List options")
        }
    }) {
        MoversDropdownMenuItem(
            text = { Text("Pick a film") },
            enabled = !state.busy && state.content?.movies?.any { it.id !in state.removing } == true,
            onClick = {
                menu = false
                viewModel.pickMovie()
                    ?.let { navigator.navigate(MovieDetailsKey(it, targetListId = listId)) }
            },
        )
        MoversDropdownMenuItem(
            text = { Text("Import / export") },
            onClick = {
                menu = false
                navigator.navigate(ListTransferKey(listId))
            })
        MoversDropdownMenuItem(text = { Text("Edit list") }, onClick = {
            menu = false
            onEdit()
        })
        MoversDropdownMenuItem(
            text = { Text("Rename list") },
            onClick = {
                menu = false
                viewModel.clearError()
                onRename()
            })
        SharingOptions(listId, list?.shared != null, navigator, onDismiss = { menu = false })
        HorizontalDivider()
        MoversDropdownMenuItem(
            text = {
                Text(
                    if (list?.shared != null) "Leave list" else "Delete list",
                    color = MaterialTheme.colorScheme.error
                )
            },
            onClick = {
                menu = false
                viewModel.clearError()
                onDelete()
            })
    }
}

@Composable
private fun SharingOptions(listId: String, shared: Boolean, navigator: Navigator, onDismiss: () -> Unit) {
    MoversDropdownMenuItem(
        text = { Text(if (shared) "Share link" else "Share list") },
        onClick = {
            onDismiss()
            navigator.navigate(
                SharedListKey(
                    listId,
                    SharedSection.Sharing
                )
            )
        })
    if (shared) {
        MoversDropdownMenuItem(
            text = { Text("Members") },
            onClick = {
                onDismiss()
                navigator.navigate(
                    SharedListKey(
                        listId,
                        SharedSection.Members
                    )
                )
            })
        MoversDropdownMenuItem(
            text = { Text("Activity") },
            onClick = {
                onDismiss()
                navigator.navigate(
                    SharedListKey(
                        listId,
                        SharedSection.Activity
                    )
                )
            })
    }
}

package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jcotters.contract.watchlists.data.FilmContribution
import com.jcotters.contract.watchlists.domain.WatchlistContent
import com.jcotters.presentation.MovieListRow
import com.jcotters.presentation.MovieMembershipButton
import com.jcotters.presentation.MoviePosterCard
import com.jcotters.presentation.MovieSwipeActions
import com.jcotters.presentation.moviePosterTransitionModifier

@Composable
internal fun WatchlistMovies(
    content: WatchlistContent,
    showGrid: Boolean,
    listState: LazyListState,
    gridState: LazyGridState,
    stackId: String,
    editing: Boolean,
    enabled: Boolean,
    removing: Set<Int>,
    onOpen: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onSwipeRemove: (Int) -> Unit,
    onAddToLists: (Int) -> Unit,
    onContribution: (FilmContribution) -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 24.dp,
    emptyMessage: String? = null,
    listFooter: LazyListScope.() -> Unit = {},
    gridFooter: LazyGridScope.() -> Unit = {},
) {
    val removeButton: @Composable (Int, String) -> Unit = { id, title ->
        if (editing) {
            MovieMembershipButton(
                movieTitle = title,
                isAdded = true,
                onClick = { onRemove(id) },
                enabled = enabled,
                saving = id in removing,
            )
        }
    }
    if (showGrid) {
        WatchlistGrid(
            content, gridState, stackId, editing, removeButton, onOpen, onContribution,
            modifier, bottomContentPadding, emptyMessage, gridFooter,
        )
    } else {
        WatchlistRows(
            content, listState, stackId, enabled, removing, onOpen, onSwipeRemove, onAddToLists,
            onContribution, removeButton, modifier, bottomContentPadding, emptyMessage, listFooter,
        )
    }
}

@Composable
private fun WatchlistGrid(
    content: WatchlistContent,
    gridState: LazyGridState,
    stackId: String,
    editing: Boolean,
    removeButton: @Composable (Int, String) -> Unit,
    onOpen: (Int) -> Unit,
    onContribution: (FilmContribution) -> Unit,
    modifier: Modifier,
    bottomContentPadding: Dp,
    emptyMessage: String?,
    footer: LazyGridScope.() -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        state = gridState,
        modifier = modifier,
        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = bottomContentPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (emptyMessage != null) item(key = "empty-list", span = { GridItemSpan(maxLineSpan) }) {
            androidx.compose.material3.Text(emptyMessage, Modifier.padding(16.dp))
        }
        gridItems(content.movies, key = { it.id }) { movie ->
            MoviePosterCard(
                movie = movie,
                onOpen = { onOpen(movie.id) },
                posterModifier = moviePosterTransitionModifier(movie.id, stackId),
                footerContent = content.contributions[movie.id]?.let { contribution ->
                    { FilmContributorButton(contribution, onClick = { onContribution(contribution) }) }
                },
                posterOverlay = {
                    if (editing) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        ) { removeButton(movie.id, movie.title) }
                    }
                },
            )
        }
        footer()
    }
}

@Composable
private fun WatchlistRows(
    content: WatchlistContent,
    listState: LazyListState,
    stackId: String,
    enabled: Boolean,
    removing: Set<Int>,
    onOpen: (Int) -> Unit,
    onSwipeRemove: (Int) -> Unit,
    onAddToLists: (Int) -> Unit,
    onContribution: (FilmContribution) -> Unit,
    removeButton: @Composable (Int, String) -> Unit,
    modifier: Modifier,
    bottomContentPadding: Dp,
    emptyMessage: String?,
    footer: LazyListScope.() -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = bottomContentPadding),
    ) {
        if (emptyMessage != null) item(key = "empty-list") {
            androidx.compose.material3.Text(emptyMessage, Modifier.padding(16.dp))
        }
        items(content.movies, key = { it.id }) { movie ->
            MovieSwipeActions(
                movieTitle = movie.title,
                onOpen = { onOpen(movie.id) },
                onAddToList = { onAddToLists(movie.id) },
                onRemove = { onSwipeRemove(movie.id) },
                enabled = enabled && movie.id !in removing,
                modifier = Modifier.animateItem(),
            ) { swipeModifier, openMovie ->
                Column(swipeModifier) {
                    MovieListRow(
                        movie = movie,
                        posterWidth = 96.dp * 1.2f,
                        modifier = Modifier.fillMaxWidth(),
                        posterModifier = moviePosterTransitionModifier(movie.id, stackId),
                        onOpen = openMovie,
                        footerContent = content.contributions[movie.id]?.let { contribution ->
                            { FilmContributorButton(contribution, onClick = { onContribution(contribution) }) }
                        },
                    ) { removeButton(movie.id, movie.title) }
                    HorizontalDivider()
                }
            }
        }
        footer()
    }
}

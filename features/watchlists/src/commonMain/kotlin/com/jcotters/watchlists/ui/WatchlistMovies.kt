package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.ui.unit.dp
import com.jcotters.contract.watchlists.data.FilmContribution
import com.jcotters.contract.watchlists.domain.WatchlistContent
import com.jcotters.presentation.MovieListRow
import com.jcotters.presentation.MovieMembershipButton
import com.jcotters.presentation.MoviePosterCard
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
    onContribution: (FilmContribution) -> Unit,
    modifier: Modifier = Modifier,
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
        LazyVerticalGrid(
            columns = GridCells.Adaptive(140.dp),
            state = gridState,
            modifier = modifier,
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            gridItems(content.movies, key = { it.id }) { movie ->
                MoviePosterCard(
                    movie = movie,
                    onOpen = { onOpen(movie.id) },
                    posterModifier = moviePosterTransitionModifier(movie.id, stackId),
                    posterOverlay = {
                        ContributorBadge(content.contributions[movie.id], onContribution)
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
        }
    } else {
        LazyColumn(state = listState, modifier = modifier.padding(horizontal = 8.dp)) {
            items(content.movies, key = { it.id }) { movie ->
                MovieListRow(
                    movie = movie,
                    modifier = Modifier.height(180.dp).fillMaxWidth(),
                    posterModifier = moviePosterTransitionModifier(movie.id, stackId),
                    onOpen = { onOpen(movie.id) },
                    posterOverlay = { ContributorBadge(content.contributions[movie.id], onContribution) },
                ) { removeButton(movie.id, movie.title) }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun BoxScope.ContributorBadge(contribution: FilmContribution?, onClick: (FilmContribution) -> Unit) {
    if (contribution != null) {
        FilmContributorButton(
            contribution = contribution,
            onClick = { onClick(contribution) },
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

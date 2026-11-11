package com.jcotters.catalogue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.presentation.AddToListButton
import com.jcotters.presentation.MoviePosterCard

@Composable
internal fun PopularMoviesGrid(
    movies: LazyPagingItems<Movie>,
    state: LazyGridState,
    onMovieTapped: (Int) -> Unit,
    posterModifier: @Composable (Movie) -> Modifier,
    onAddToList: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        stickyHeader(key = "heading") { CatalogueHeading() }
        if (movies.loadState.refresh is LoadState.Error) {
            item(key = "refresh-error", span = { GridItemSpan(maxLineSpan) }) {
                CatalogueRefreshError(movies::retry)
            }
        }
        items(count = movies.itemCount, key = movies.itemKey(Movie::id)) { index ->
            val movie = movies[index] ?: return@items
            MoviePosterCard(
                movie = movie,
                posterModifier = posterModifier(movie),
                onOpen = { onMovieTapped(movie.id) },
                posterOverlay = {
                    if (onAddToList != null) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        ) { AddToListButton(onClick = { onAddToList(movie.id) }) }
                    }
                },
            )
        }
        item(key = "append-state", span = { GridItemSpan(maxLineSpan) }) {
            CatalogueAppendState(movies.loadState.append, movies::retry)
        }
    }
}

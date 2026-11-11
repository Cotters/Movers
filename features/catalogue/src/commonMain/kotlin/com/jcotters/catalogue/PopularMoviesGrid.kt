package com.jcotters.catalogue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.presentation.AddToListButton
import com.jcotters.presentation.MoverImageView
import com.jcotters.presentation.moviePosterKey

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
                onAddToList = onAddToList?.let { add -> { add(movie.id) } },
            )
        }
        item(key = "append-state", span = { GridItemSpan(maxLineSpan) }) {
            CatalogueAppendState(movies.loadState.append, movies::retry)
        }
    }
}

@Composable
private fun MoviePosterCard(
    movie: Movie,
    posterModifier: Modifier,
    onOpen: () -> Unit,
    onAddToList: (() -> Unit)?,
) {
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen)) {
        Box {
            MoverImageView(
                posterUrl = movie.posterUrl.orEmpty(),
                contentDescription = "Movie poster for ${movie.title}",
                sharedImageKey = moviePosterKey(movie.id),
                modifier = posterModifier.fillMaxWidth().aspectRatio(2f / 3f),
            )
            if (onAddToList != null) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                ) { AddToListButton(onClick = onAddToList) }
            }
        }
        Text(
            movie.title,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (movie.releaseDate.isNotBlank()) {
            Text(
                movie.releaseDate.take(4),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

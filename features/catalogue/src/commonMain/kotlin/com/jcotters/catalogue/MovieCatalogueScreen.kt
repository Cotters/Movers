package com.jcotters.catalogue

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.detail.domain.models.PreviewMovies
import com.jcotters.designsystem.MoversButton
import com.jcotters.designsystem.MoversCard
import com.jcotters.designsystem.MoversIconButton
import com.jcotters.designsystem.MoversTopBar
import com.jcotters.features.catalogue.resources.Res
import com.jcotters.features.catalogue.resources.cached_movies_refresh_error
import com.jcotters.presentation.MovieLayoutToggle
import com.jcotters.presentation.MovieListRow
import com.jcotters.presentation.MovieSwipeActions
import com.jcotters.presentation.ScrollToTopButton
import com.jcotters.presentation.resources.search
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MovieCatalogueScreen(
    movies: LazyPagingItems<Movie>,
    posterModifier: @Composable (Movie) -> Modifier,
    onMovieTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onAddToList: ((Int) -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    var showGrid by rememberSaveable { mutableStateOf(false) }

    val showScrollToTopFab by remember {
        derivedStateOf {
            (if (showGrid) gridState.firstVisibleItemIndex else listState.firstVisibleItemIndex) > 15
        }
    }

    Scaffold(
        topBar = {
            MoversTopBar(title = { Text("Movers") }, actions = {
                MovieLayoutToggle(showGrid = showGrid, onClick = {
                    // Both layouts use the same item order, including full-width status rows.
                    val anchor = if (showGrid) gridState.firstVisibleItemIndex else listState.firstVisibleItemIndex
                    if (showGrid) listState.requestScrollToItem(anchor) else gridState.requestScrollToItem(anchor)
                    showGrid = !showGrid
                })
                if (onSearch != null) MoversIconButton(onClick = onSearch) {
                    Icon(painterResource(com.jcotters.presentation.resources.Res.drawable.search), "Search movies")
                }
            })
        },
        floatingActionButton = {
            ScrollToTopButton(
                visible = showScrollToTopFab,
                onClick = {
                    coroutineScope.launch {
                        if (showGrid) gridState.animateScrollToItem(0) else listState.animateScrollToItem(0)
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = movies.loadState.refresh is LoadState.Loading,
            onRefresh = movies::refresh,
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            PopularMoviesContent(
                modifier = Modifier,
                listState = listState,
                gridState = gridState,
                showGrid = showGrid,
                movies = movies,
                onMovieTapped = onMovieTapped,
                posterModifier = posterModifier,
                onAddToList = onAddToList,
            )
        }
    }
}

@Composable
private fun PopularMoviesContent(
    modifier: Modifier,
    listState: LazyListState,
    gridState: LazyGridState,
    showGrid: Boolean,
    movies: LazyPagingItems<Movie>,
    onMovieTapped: (Int) -> Unit,
    posterModifier: @Composable (Movie) -> Modifier,
    onAddToList: ((Int) -> Unit)?,
) {
    val refreshState = movies.loadState.refresh

    when {
        // Load state can become NotLoading before the first item snapshot arrives. Never attach
        // restored scroll state to a header/footer-only layout: it clamps the saved index to zero.
        movies.itemCount == 0 -> EmptyCatalogueContent(movies, showGrid, modifier)

        showGrid -> PopularMoviesGrid(movies, gridState, onMovieTapped, posterModifier, onAddToList, modifier)

        else -> LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize().padding(horizontal = 8.dp),
        ) {
            stickyHeader(key = "heading", contentType = "heading") { CatalogueHeading() }
            if (refreshState is LoadState.Error) {
                item(key = "refresh-error", contentType = "status") {
                    CatalogueRefreshError(movies::retry)
                }
            }
            items(
                count = movies.itemCount,
                key = movies.itemKey(Movie::id),
                contentType = movies.itemContentType { "movie" },
            ) { index ->
                val movie = movies[index]
                if (movie == null) {
                    CatalogueMoviePlaceholder(grid = false)
                } else MovieSwipeActions(
                    movieTitle = movie.title,
                    onOpen = { onMovieTapped(movie.id) },
                    onAddToList = onAddToList?.let { add -> { add(movie.id) } },
                ) { swipeModifier, openMovie ->
                    Column(swipeModifier) {
                        MovieListRow(
                            movie = movie,
                            posterWidth = cataloguePosterWidth,
                            modifier = Modifier.fillMaxWidth(),
                            posterModifier = posterModifier(movie),
                            onOpen = openMovie,
                        )
                        HorizontalDivider()
                    }
                }
            }
            item(key = "append-state", contentType = "status") {
                CatalogueAppendState(movies.loadState.append, movies::retry)
            }
        }
    }
}

@Composable
private fun EmptyCatalogueContent(movies: LazyPagingItems<Movie>, showGrid: Boolean, modifier: Modifier) {
    val refreshState = movies.loadState.refresh
    if (refreshState is LoadState.Error) {
        ErrorItem(refreshState.error.message ?: "Failed to load movies", movies::retry, modifier)
    } else {
        Column(modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = if (showGrid) 8.dp else 0.dp)) {
            CatalogueHeading()
            if (refreshState is LoadState.Loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LinearProgressIndicator() }
            } else {
                CatalogueAppendState(movies.loadState.append, movies::retry)
            }
        }
    }
}

@Composable
internal fun CatalogueHeading() {
    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        Text("Popular Movies", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
internal fun CatalogueRefreshError(onRetry: () -> Unit) {
    ErrorItem(stringResource(Res.string.cached_movies_refresh_error), onRetry)
}

@Composable
internal fun CatalogueAppendState(state: LoadState, onRetry: () -> Unit) {
    when (state) {
        is LoadState.Loading -> Box(
            Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) { LinearProgressIndicator() }

        is LoadState.Error -> ErrorItem(state.error.message ?: "Failed to load more", onRetry)
        else -> Unit
    }
}

@Composable
fun ErrorItem(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    MoversCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            MoversButton(
                onClick = onRetry,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Retry")
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun MovieCatalogueScreenPreview() {
    val pagingData = PagingData.from(PreviewMovies.movies)
    val lazyPagingItems = flowOf(pagingData)
        .collectAsLazyPagingItems()
    com.jcotters.designsystem.MoversDesignTheme {
        MovieCatalogueScreen(
            movies = lazyPagingItems,
            posterModifier = { _ -> Modifier },
            onMovieTapped = { _ -> },
        )
    }
}

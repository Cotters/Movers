package com.jcotters.catalogue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.detail.domain.models.PreviewMovies
import com.jcotters.features.catalogue.R
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MovieCatalogueScreen(
    movies: LazyPagingItems<Movie>,
    posterModifier: @Composable (Movie) -> Modifier,
    onMovieTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    val showScrollToTopFab by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 15 }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Movers") }) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToTopFab,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(index = 0)
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_up),
                        contentDescription = "Scroll to top button",
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = movies.loadState.refresh is LoadState.Loading,
            onRefresh = movies::refresh,
            state = pullToRefreshState,
            modifier = Modifier.padding(innerPadding)
        ) {
            PopularMoviesList(
                modifier = Modifier,
                listState = listState,
                movies = movies,
                onMovieTapped = onMovieTapped,
                posterModifier = posterModifier,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun PopularMoviesList(
    modifier: Modifier,
    listState: LazyListState,
    movies: LazyPagingItems<Movie>,
    onMovieTapped: (Int) -> Unit,
    posterModifier: @Composable (Movie) -> Modifier,
) {
    val refreshState = movies.loadState.refresh
    val appendState = movies.loadState.append

    when {
        refreshState is LoadState.Error && movies.itemCount == 0 -> {
            val error = refreshState.error
            ErrorItem(
                message = error.message ?: "Failed to load movies",
                onRetry = movies::retry,
            )
        }

        else -> LazyColumn(
            state = listState,
            modifier = modifier.padding(horizontal = 8.dp),
        ) {
            stickyHeader {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Text(text = "Popular Movies", style = MaterialTheme.typography.titleLarge)
                }
            }
            if (refreshState is LoadState.Error) {
                item(key = "refresh-error") {
                    ErrorItem(
                        message = stringResource(R.string.cached_movies_refresh_error),
                        onRetry = movies::retry,
                    )
                }
            }
            items(
                count = movies.itemCount,
                key = movies.itemKey(Movie::id),
            ) { index ->
                val movie = movies[index] ?: return@items
                MovieCatalogueListItem(
                    movie = movie,
                    modifier = Modifier
                        .height(180.dp)
                        .fillParentMaxWidth()
                        .clickable { onMovieTapped(movie.id) },
                    posterModifier = posterModifier(movie)
                )
                HorizontalDivider()
            }
            item {
                when (appendState) {
                    is LoadState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LinearProgressIndicator()
                        }
                    }

                    is LoadState.Error -> {
                        ErrorItem(
                            message = appendState.error.message ?: "Failed to load more",
                            onRetry = movies::retry
                        )
                    }

                    else -> {}
                }
            }
        }
    }
}

@Composable
fun ErrorItem(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
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
            Button(
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
    MovieCatalogueScreen(
        movies = lazyPagingItems,
        posterModifier = { _ -> Modifier },
        onMovieTapped = { _ -> },
    )
}

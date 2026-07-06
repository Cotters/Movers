package com.jcotters.movie.catalogue.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.jcotters.movie.R
import com.jcotters.movie.detail.domain.models.Movie
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MovieCatalogueScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    isAuthenticated: Boolean,
    movies: LazyPagingItems<Movie>,
    onMovieTapped: (Int) -> Unit,
    onAccountTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    val showScrollToTopFab by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 15 }
    }

    val accountIcon = if (isAuthenticated) R.drawable.person_filled else R.drawable.person_outline
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Movers") },
                actions = {
                    IconButton(onClick = onAccountTapped) {
                        Icon(
                            painter = painterResource(accountIcon),
                            contentDescription = "Account top bar button",
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            )
        },
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
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = movies.loadState.refresh is LoadState.Loading,
            onRefresh = { movies.refresh() },
            state = pullToRefreshState,
            modifier = Modifier.padding(innerPadding)
        ) {
            PopularMoviesList(
                modifier = modifier,
                listState = listState,
                movies = movies,
                onMovieTapped = onMovieTapped,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
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
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val refreshState = movies.loadState.refresh
    val appendState = movies.loadState.append

    when (refreshState) {
        is LoadState.Error -> {
            val error = (movies.loadState.refresh as LoadState.Error).error
            ErrorItem(
                message = error.message ?: "Failed to load movies",
                onRetry = { movies.retry() }
            )
        }

        else -> LazyColumn(
            state = listState,
            modifier = modifier,
        ) {
            stickyHeader {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Text(text = "Total Items: ${movies.itemCount}")
                }
            }
            items(
                count = movies.itemCount,
                key = { index -> movies[index]?.id ?: index },
            ) { index ->
                movies[index]?.let { movie ->
                    MovieCatalogueListItem(
                        movie = movie,
                        modifier = Modifier
                            .height(180.dp)
                            .fillParentMaxWidth()
                            .clickable { onMovieTapped(movie.id) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                }
                Text("Index: $index")
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
                        val error = (movies.loadState.append as LoadState.Error).error
                        ErrorItem(
                            message = error.message ?: "Failed to load more",
                            onRetry = { movies.retry() }
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

//@OptIn(ExperimentalSharedTransitionApi::class)
//@Preview
//@Composable
//private fun MovieCatalogueScreenPreview() {
//  SharedTransitionLayout {
//    AnimatedVisibility(visible = true) {
//      MovieCatalogueScreen(
//        sharedTransitionScope = this@SharedTransitionLayout,
//        animatedVisibilityScope = this,
//        isAuthenticated = false,
//        movies = PreviewMovies.movies,
//        onMovieTapped = { movieId -> },
//        onAccountTapped = {},
//      )
//    }
//  }
//}
package com.jcotters.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.jcotters.designsystem.MoversIconButton
import com.jcotters.designsystem.MoversTextButton
import com.jcotters.designsystem.MoversTopBar
import com.jcotters.details.navigation.MovieDetailsKey
import com.jcotters.navigation.Navigator
import com.jcotters.presentation.AddToListButton
import com.jcotters.presentation.MovieMembershipButton
import com.jcotters.presentation.MovieSearchField
import com.jcotters.presentation.MovieSearchResult
import com.jcotters.presentation.moviePosterTransitionModifier
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.arrow_back
import com.jcotters.watchlists.navigation.AddToListsKey
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MovieSearchRoute(listId: String?, navigator: Navigator, stackId: String) {
    val viewModel: MovieSearchViewModel = koinViewModel()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchedQuery by viewModel.searchedQuery.collectAsStateWithLifecycle()
    val actions by viewModel.actionsState.collectAsStateWithLifecycle()
    val added by viewModel.addedIds.collectAsStateWithLifecycle()
    val movies = viewModel.movies.collectAsLazyPagingItems()
    val scroll = rememberLazyListState()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(listId) { viewModel.setTarget(listId) }
    LaunchedEffect(Unit) { if (query.isBlank()) focus.requestFocus() }
    val target = actions.lists.firstOrNull { it.id == listId }
    val missing = listId != null && !actions.loadingLists && actions.error == null && target == null
    Scaffold(topBar = {
        MoversTopBar(title = { Text(target?.let { "Add to ${it.name}" } ?: "Search movies") }, navigationIcon = {
            MoversIconButton(onClick = navigator::goBack) { Icon(painterResource(Res.drawable.arrow_back), "Back") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MovieSearchField(
                value = query,
                onValueChange = { value ->
                    if (value.trim() != query.trim()) scope.launch { scroll.scrollToItem(0) }
                    viewModel.changeQuery(value)
                },
                onSearch = { viewModel.submit(); keyboard?.hide() },
                modifier = Modifier.padding(12.dp).focusRequester(focus),
            )
            actions.error?.let {
                Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error)
                MoversTextButton(onClick = { viewModel.clearError(); viewModel.retryLists() }) { Text("Dismiss") }
            }
            when {
                missing -> Text("This list no longer exists.", Modifier.padding(16.dp))
                query.isBlank() -> Text("Search for a movie to add to your lists.", Modifier.padding(16.dp))
                query.trim() != searchedQuery -> LinearProgressIndicator(Modifier.fillMaxWidth())
                movies.loadState.refresh is LoadState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                movies.loadState.refresh is LoadState.Error && movies.itemCount == 0 -> {
                    Text((movies.loadState.refresh as LoadState.Error).error.message ?: "Unable to search.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                    MoversTextButton(onClick = movies::retry) { Text("Retry") }
                }
                movies.itemCount == 0 -> Text("No movies found.", Modifier.padding(16.dp))
                else -> LazyColumn(state = scroll) {
                    items(movies.itemCount, key = movies.itemKey { it.id }) { index ->
                        val movie = movies[index] ?: return@items
                        MovieSearchResult(movie,
                            posterModifier = moviePosterTransitionModifier(movie.id, stackId),
                            onOpen = { keyboard?.hide(); navigator.navigate(MovieDetailsKey(movie.id, targetListId = listId)) }) {
                            if (listId == null) {
                                AddToListButton(onClick = { keyboard?.hide(); navigator.navigate(AddToListsKey(movie.id)) })
                            } else {
                                val isAdded = movie.id in added
                                MovieMembershipButton(
                                    movieTitle = movie.title,
                                    isAdded = isAdded,
                                    onClick = { viewModel.setMembership(movie.id, !isAdded) },
                                    enabled = !actions.membershipLoading && !actions.membershipReadFailed,
                                    saving = movie.id in actions.saving,
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                    item {
                        when (val append = movies.loadState.append) {
                            is LoadState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                            is LoadState.Error -> Column(Modifier.padding(16.dp)) {
                                Text(append.error.message ?: "Unable to load more movies.", color = MaterialTheme.colorScheme.error)
                                MoversTextButton(onClick = movies::retry) { Text("Retry") }
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

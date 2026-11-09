package com.jcotters.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.jcotters.details.navigation.MovieDetailsKey
import com.jcotters.navigation.Navigator
import com.jcotters.presentation.AddToListButton
import com.jcotters.presentation.MovieListRow
import com.jcotters.presentation.moviePosterTransitionModifier
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.arrow_back
import com.jcotters.watchlists.navigation.AddToListsKey
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.launch

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
        TopAppBar(title = { Text(target?.let { "Add to ${it.name}" } ?: "Search movies") }, navigationIcon = {
            IconButton(onClick = navigator::goBack) { Icon(painterResource(Res.drawable.arrow_back), "Back") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query, onValueChange = { value ->
                    if (value.trim() != query.trim()) scope.launch { scroll.scrollToItem(0) }
                    viewModel.changeQuery(value)
                }, label = { Text("Search films") },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(12.dp).focusRequester(focus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.submit(); keyboard?.hide() }),
                trailingIcon = { if (query.isNotEmpty()) TextButton(onClick = { viewModel.changeQuery("") }) { Text("Clear") } },
            )
            actions.error?.let {
                Text(it, Modifier.padding(12.dp))
                TextButton(onClick = { viewModel.clearError(); viewModel.retryLists() }) { Text("Dismiss") }
            }
            when {
                missing -> Text("This list no longer exists.", Modifier.padding(16.dp))
                query.isBlank() -> Text("Search for a movie to add to your lists.", Modifier.padding(16.dp))
                query.trim() != searchedQuery -> LinearProgressIndicator(Modifier.fillMaxWidth())
                movies.loadState.refresh is LoadState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                movies.loadState.refresh is LoadState.Error && movies.itemCount == 0 -> {
                    Text((movies.loadState.refresh as LoadState.Error).error.message ?: "Unable to search.", Modifier.padding(16.dp))
                    TextButton(onClick = movies::retry) { Text("Retry") }
                }
                movies.itemCount == 0 -> Text("No movies found.", Modifier.padding(16.dp))
                else -> LazyColumn(state = scroll) {
                    items(movies.itemCount, key = movies.itemKey { it.id }) { index ->
                        val movie = movies[index] ?: return@items
                        MovieListRow(movie, Modifier.fillMaxWidth().height(180.dp),
                            posterModifier = moviePosterTransitionModifier(movie.id, stackId),
                            onOpen = { keyboard?.hide(); navigator.navigate(MovieDetailsKey(movie.id, targetListId = listId)) }) {
                            if (listId == null) {
                                AddToListButton(onClick = { keyboard?.hide(); navigator.navigate(AddToListsKey(movie.id)) })
                            } else {
                                TextButton(onClick = { viewModel.add(movie.id) }, enabled = movie.id !in added && movie.id !in actions.saving) {
                                    Text(if (movie.id in added) "Added" else if (movie.id in actions.saving) "Adding…" else "Add")
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                    item {
                        when (val append = movies.loadState.append) {
                            is LoadState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                            is LoadState.Error -> Column(Modifier.padding(16.dp)) {
                                Text(append.error.message ?: "Unable to load more movies.")
                                TextButton(onClick = movies::retry) { Text("Retry") }
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

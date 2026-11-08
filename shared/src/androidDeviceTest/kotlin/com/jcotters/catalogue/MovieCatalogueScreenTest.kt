package com.jcotters.catalogue

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.jcotters.contract.detail.domain.models.Movie
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalSharedTransitionApi::class)
class MovieCatalogueScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun failedRefreshKeepsCachedMoviesVisibleWithRetry() {
        showMovies(listOf(Movie(7, "Saved movie", "Overview", "2026")))
        compose.onNodeWithText("Saved movie").assertIsDisplayed()
        compose.onNodeWithText("Couldn’t refresh movies. Showing saved results.").assertIsDisplayed()
        compose.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun failedRefreshWithoutCacheShowsErrorAndRetry() {
        showMovies(emptyList())
        compose.onNodeWithText("Offline").assertIsDisplayed()
        compose.onNodeWithText("Retry").assertIsDisplayed()
    }

    private fun showMovies(movies: List<Movie>) {
        val pagingData = PagingData.from(
            data = movies,
            sourceLoadStates = LoadStates(
                refresh = LoadState.Error(IOException("Offline")),
                prepend = LoadState.NotLoading(true),
                append = LoadState.NotLoading(true),
            ),
        )
        val flow = flowOf(pagingData)
        compose.setContent {
            MaterialTheme {
                MovieCatalogueScreen(
                    movies = flow.collectAsLazyPagingItems(),
                    posterModifier = { Modifier },
                    onMovieTapped = {},
                )
            }
        }
    }
}

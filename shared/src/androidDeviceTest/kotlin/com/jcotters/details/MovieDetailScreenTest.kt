package com.jcotters.details

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ColorImage
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.detail.domain.models.MovieGenre
import com.jcotters.details.ui.MovieDetailScreen
import com.jcotters.details.ui.MovieDetailViewEvent
import com.jcotters.details.ui.MovieDetailViewState
import com.jcotters.presentation.moviePosterKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MovieDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun movieDetailsDisplayed() {
        val viewState = MovieDetailViewState(isLoading = false, movie = movie)
        composeTestRule.setContent {
            MovieDetailScreen(
                onViewEvent = {},
                viewState = viewState,
            )
        }
        composeTestRule
            .onNodeWithText(movie.title)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(movie.synopsis)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Released ${movie.releaseDate}")
            .assertIsDisplayed()
    }

    @Test
    fun knownContentAndPosterRemainWhileMissingDetailsLoad() {
        val state = mutableStateOf(MovieDetailViewState(isLoading = false, movie = movie, isRefreshingDetails = true))
        composeTestRule.setContent { MovieDetailScreen({}, state.value) }
        composeTestRule.onNodeWithText(movie.title).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Movie poster for ${movie.title}").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Loading runtime").assertExists()
        composeTestRule.runOnIdle {
            state.value = state.value.copy(isRefreshingDetails = false, movie = movie.copy(
                runtimeMinutes = 125, genres = listOf(MovieGenre(18, "Drama")), detailsFetchedAt = 123,
            ))
        }
        composeTestRule.onNodeWithText("2h 5m").assertExists()
        composeTestRule.onNodeWithText("Drama").assertExists()
        composeTestRule.onAllNodesWithContentDescription("Loading runtime").assertCountEquals(0)
        composeTestRule.onAllNodesWithContentDescription("Loading tagline").assertCountEquals(0)
        composeTestRule.onNodeWithContentDescription("Movie poster for ${movie.title}").assertIsDisplayed()
    }

    @Test
    fun cachedDetailsRemainVisibleDuringRefresh() {
        composeTestRule.setContent {
            MovieDetailScreen({}, MovieDetailViewState(isLoading = false, isRefreshingDetails = true,
                movie = movie.copy(runtimeMinutes = 125, detailsFetchedAt = 123)))
        }
        composeTestRule.onNodeWithText("2h 5m").assertExists()
        composeTestRule.onAllNodesWithContentDescription("Loading runtime").assertCountEquals(0)
        composeTestRule.onNodeWithContentDescription("Refreshing movie details").assertExists()
    }

    @Test
    fun failedEnrichmentKeepsContentAndOffersInlineRetry() {
        var retried = false
        composeTestRule.setContent {
            MovieDetailScreen({ event -> retried = event == MovieDetailViewEvent.RetryDetails },
                MovieDetailViewState(isLoading = false, movie = movie,
                    detailsError = "Unable to load additional movie details."))
        }
        composeTestRule.onNodeWithText(movie.title).assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("Loading runtime").assertCountEquals(0)
        composeTestRule.onNodeWithText("Retry").performScrollTo().performClick()
        composeTestRule.runOnIdle { org.junit.Assert.assertTrue(retried) }
    }

    @Test
    fun noCacheFailureShowsARetryableError() {
        composeTestRule.setContent {
            MovieDetailScreen({}, MovieDetailViewState(isLoading = false,
                detailsError = "Unable to load additional movie details."))
        }
        composeTestRule.onNodeWithText("Unable to load additional movie details.").assertExists()
        composeTestRule.onNodeWithText("Retry").assertExists()
    }

    @Test
    fun cachedPosterDisplaysBeforeRoomSuppliesMovieData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val memoryCache = requireNotNull(SingletonImageLoader.get(context).memoryCache)
        val key = MemoryCache.Key(moviePosterKey(7))
        memoryCache[key] = MemoryCache.Value(ColorImage(Color.Red.toArgb()))
        try {
            composeTestRule.setContent {
                MovieDetailScreen(
                    onViewEvent = {},
                    viewState = MovieDetailViewState(),
                    movieId = 7,
                    posterModifier = Modifier.testTag("detail-poster"),
                )
            }
            val pixels = composeTestRule.onNodeWithTag("detail-poster").captureToImage().toPixelMap()
            assertEquals(Color.Red, pixels[pixels.width / 2, pixels.height / 2])
        } finally {
            memoryCache.remove(key)
        }
    }

    private companion object {
        val movie = Movie(
            id = 1,
            title = "The Godfather",
            synopsis = "The ageing patriarch of an organised crime dynasty...",
            releaseDate = "1972-03-24",
            posterUrl = "poster.jpg",
        )
    }
}

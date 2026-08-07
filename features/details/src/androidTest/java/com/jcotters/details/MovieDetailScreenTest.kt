package com.jcotters.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.details.ui.MovieDetailScreen
import com.jcotters.details.ui.MovieDetailViewState
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
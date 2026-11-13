package com.jcotters.details.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.detail.domain.models.MovieGenre

private val previewMovie = Movie(
    id = 1, title = "Preview: The Movie", synopsis = "A preview to die for...", releaseDate = "2026-10-07",
    tagline = "Every frame tells a story.", runtimeMinutes = 125,
    genres = listOf(
        MovieGenre(18, "Drama"), MovieGenre(12, "Adventure"),
        MovieGenre(878, "Science Fiction"), MovieGenre(9648, "Mystery"),
    ),
    voteAverage = 7.8, voteCount = 1234, status = "Released", originalLanguage = "en", detailsFetchedAt = 1,
)

@Preview(name = "Completed", widthDp = 360, heightDp = 800)
@Composable
private fun MovieDetailScreenPreview() {
    DetailPreview(MovieDetailViewState(isLoading = false, movie = previewMovie))
}

@Preview(name = "Initial loading", widthDp = 360, heightDp = 800)
@Composable
private fun InitialLoadingPreview() {
    DetailPreview(MovieDetailViewState(isRefreshingDetails = true))
}

@Preview(name = "Partial content", widthDp = 360, heightDp = 800)
@Composable
private fun PartialContentPreview() {
    DetailPreview(
        MovieDetailViewState(
            isLoading = false, isRefreshingDetails = true,
            movie = Movie(1, "Preview: The Movie", "A preview to die for...", "2026-10-07"),
        )
    )
}

@Preview(name = "Fetch failed", widthDp = 360, heightDp = 800)
@Composable
private fun FetchFailedPreview() {
    DetailPreview(
        MovieDetailViewState(
            isLoading = false, movie = previewMovie,
            detailsError = "Unable to load additional movie details.",
        )
    )
}

@Preview(name = "Long title · large text", widthDp = 320, heightDp = 800, fontScale = 1.5f)
@Composable
private fun LongTitlePreview() {
    DetailPreview(
        MovieDetailViewState(
            isLoading = false, movie = previewMovie.copy(
                title = "A Very Long Movie Title That Needs Room to Wrap",
                tagline = "A longer subtitle that should remain readable beside the film poster.",
            )
        )
    )
}

@Preview(name = "Wide", widthDp = 840, heightDp = 640)
@Composable
private fun WidePreview() {
    DetailPreview(MovieDetailViewState(isLoading = false, movie = previewMovie))
}

@Composable
private fun DetailPreview(state: MovieDetailViewState) {
    com.jcotters.designsystem.MoversDesignTheme {
        MovieDetailScreen(onViewEvent = {}, viewState = state, onBack = {}, onAddToList = {})
    }
}

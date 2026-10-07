package com.jcotters.details.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.detail.domain.models.MovieGenre
import com.jcotters.presentation.MoverImageView
import com.jcotters.presentation.R
import com.jcotters.presentation.moviePosterKey
import java.util.Locale

@Composable
fun MovieDetailScreen(
    onViewEvent: (MovieDetailViewEvent) -> Unit,
    viewState: MovieDetailViewState,
    modifier: Modifier = Modifier,
    posterModifier: Modifier = Modifier,
    movieId: Int? = viewState.movie?.id,
) {
    val movie = viewState.movie
    val loading = viewState.isLoading || viewState.isRefreshingDetails
    val loadingMissingDetails = loading && movie?.detailsFetchedAt == null
    val placeholderAlpha = if (loadingMissingDetails) {
        val transition = rememberInfiniteTransition(label = "Detail loading")
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "Placeholder opacity",
        )
    } else remember { mutableFloatStateOf(0.35f) }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            MovieHeader(movie, viewState, onViewEvent, posterModifier, movieId)
            Column(
                modifier = Modifier.padding(top = 50.dp).padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DetailValue("title", movie?.title, loadingMissingDetails, placeholderAlpha,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
                    DetailValue("tagline", movie?.tagline, loadingMissingDetails, placeholderAlpha)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        movie?.releaseDate?.takeIf { it.isNotBlank() }?.let {
                            Text("Released $it", style = MaterialTheme.typography.bodyMedium)
                        }
                        DetailValue("runtime", movie?.runtimeMinutes?.let(::formatRuntime),
                            loadingMissingDetails, placeholderAlpha, modifier = Modifier)
                    }
                    val genres = movie?.genres.orEmpty()
                    if (genres.isNotEmpty() || loadingMissingDetails) {
                        AnimatedContent(
                            targetState = genres,
                            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                            label = "Loaded genres",
                        ) { loadedGenres ->
                            if (loadedGenres.isNotEmpty()) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    loadedGenres.forEach { genre ->
                                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                            Text(genre.name, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                }
                            } else {
                                DetailPlaceholder("genres", placeholderAlpha)
                            }
                        }
                    }
                    DetailValue("rating", formatRating(movie), loadingMissingDetails, placeholderAlpha)
                    DetailValue("synopsis", movie?.synopsis, loadingMissingDetails, placeholderAlpha,
                        style = MaterialTheme.typography.bodyMedium)
                    DetailValue("status", movie?.status?.let { "Status: $it" }, loadingMissingDetails, placeholderAlpha)
                    DetailValue("original language", movie?.originalLanguage?.let { code ->
                        "Original language: ${Locale.forLanguageTag(code).getDisplayLanguage(Locale.getDefault()).ifBlank { code }}"
                    }, loadingMissingDetails, placeholderAlpha)
                }
                if (viewState.isRefreshingDetails && movie?.detailsFetchedAt != null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = "Refreshing movie details"
                    })
                }
                val error = viewState.detailsError ?: viewState.errorMessage.takeIf { it.isNotBlank() }
                if (error != null) {
                    Column(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { onViewEvent(MovieDetailViewEvent.RetryDetails) }, enabled = !loading) {
                            Text("Retry")
                        }
                    }
                } else if (movie == null && !loading) {
                    Text("Movie details are unavailable.")
                    TextButton(onClick = { onViewEvent(MovieDetailViewEvent.RetryDetails) }) { Text("Retry") }
                }
            }
        }
    }
}

@Composable
private fun MovieHeader(
    movie: Movie?,
    viewState: MovieDetailViewState,
    onViewEvent: (MovieDetailViewEvent) -> Unit,
    posterModifier: Modifier,
    movieId: Int?,
) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
        MoverImageView(
            posterUrl = movie?.backdropUrl.orEmpty(),
            contentDescription = "Movie backdrop image for ${movie?.title.orEmpty()}",
            modifier = Modifier.fillMaxSize(),
        )
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
        )))
        MoverImageView(
            posterUrl = movie?.posterUrl.orEmpty(),
            contentDescription = movie?.let { "Movie poster for ${it.title}" } ?: "Movie poster",
            sharedImageKey = movieId?.let(::moviePosterKey),
            modifier = posterModifier.align(Alignment.BottomStart).padding(start = 24.dp)
                .offset(y = 50.dp).size(width = 120.dp, height = 180.dp),
        )
        if (movie != null) {
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).clip(CircleShape)
                .size(40.dp).background(Color.Black.copy(alpha = 0.5f))) {
                val bookmarkIcon = if (viewState.isBookmarked) R.drawable.bookmark_filled else R.drawable.bookmark_outline
                IconButton(
                    onClick = { onViewEvent(MovieDetailViewEvent.BookmarkTapped(movie.id)) },
                    enabled = viewState.isBookmarkLoaded && !viewState.isSavingBookmark,
                ) {
                    Icon(painter = painterResource(bookmarkIcon), contentDescription = "Bookmark movie button",
                        modifier = Modifier.size(25.dp), tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DetailValue(
    label: String,
    value: String?,
    loading: Boolean,
    placeholderAlpha: State<Float>,
    modifier: Modifier = Modifier.fillMaxWidth(),
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val text = value?.takeIf { it.isNotBlank() }
    if (text == null && !loading) return
    AnimatedContent(
        targetState = text,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
        label = "Loaded $label",
    ) { loadedText ->
        if (loadedText != null) {
            Text(loadedText, modifier = modifier, style = style)
        } else {
            DetailPlaceholder(label, placeholderAlpha)
        }
    }
}

@Composable
private fun DetailPlaceholder(label: String, opacity: State<Float>) {
    Spacer(modifier = Modifier.fillMaxWidth(0.65f).height(20.dp).clip(RoundedCornerShape(6.dp))
        .graphicsLayer { alpha = opacity.value }
        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        .semantics { contentDescription = "Loading $label" })
}

internal fun formatRuntime(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

internal fun formatRating(movie: Movie?): String? = when {
    movie?.voteCount == 0 -> "Not yet rated"
    movie?.voteAverage != null && movie.voteCount != null ->
        String.format(Locale.getDefault(), "TMDB %.1f/10 · %,d votes", movie.voteAverage, movie.voteCount)
    else -> null
}

private val previewMovie = Movie(
    id = 1, title = "Preview: The Movie", synopsis = "A preview to die for...", releaseDate = "2026-10-07",
    tagline = "Every frame tells a story.", runtimeMinutes = 125,
    genres = listOf(MovieGenre(18, "Drama"), MovieGenre(12, "Adventure")),
    voteAverage = 7.8, voteCount = 1234, status = "Released", originalLanguage = "en", detailsFetchedAt = 1,
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(name = "Completed")
@Composable
private fun MovieDetailScreenPreview() {
    SharedTransitionLayout {
        AnimatedVisibility(visible = true) {
            MovieDetailScreen({}, MovieDetailViewState(isLoading = false, movie = previewMovie, isBookmarked = true))
        }
    }
}

@Preview(name = "Initial loading")
@Composable
private fun InitialLoadingPreview() {
    MovieDetailScreen({}, MovieDetailViewState(isRefreshingDetails = true))
}

@Preview(name = "Partial content")
@Composable
private fun PartialContentPreview() {
    MovieDetailScreen({}, MovieDetailViewState(isLoading = false, isRefreshingDetails = true,
        movie = Movie(1, "Preview: The Movie", "A preview to die for...", "2026-10-07")))
}

@Preview(name = "Fetch failed")
@Composable
private fun FetchFailedPreview() {
    MovieDetailScreen({}, MovieDetailViewState(isLoading = false, movie = previewMovie,
        detailsError = "Unable to load additional movie details."))
}

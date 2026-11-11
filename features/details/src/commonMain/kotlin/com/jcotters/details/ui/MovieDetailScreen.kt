package com.jcotters.details.ui

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.detail.domain.models.MovieGenre
import com.jcotters.details.navigation.MovieImageType
import com.jcotters.presentation.MoverImageView
import com.jcotters.presentation.movieBackdropKey
import com.jcotters.presentation.moviePosterKey
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.add
import com.jcotters.presentation.resources.arrow_back
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun MovieDetailScreen(
    onViewEvent: (MovieDetailViewEvent) -> Unit,
    viewState: MovieDetailViewState,
    modifier: Modifier = Modifier,
    posterModifier: Modifier = Modifier,
    movieId: Int? = viewState.movie?.id,
    onBack: (() -> Unit)? = null,
    onAddToList: (() -> Unit)? = null,
    backdropModifier: Modifier = Modifier,
    onImageTapped: ((MovieImageType) -> Unit)? = null,
    trailerContent: @Composable () -> Unit = {},
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
            MovieHeader(
                movie, posterModifier, movieId, onBack, onAddToList,
                loadingMissingDetails, placeholderAlpha,
                backdropModifier, onImageTapped,
            )
            Column(
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val genres = movie?.genres.orEmpty()
                    if (genres.isNotEmpty() || loadingMissingDetails) {
                        AnimatedContent(
                            targetState = genres,
                            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                            label = "Loaded genres",
                        ) { loadedGenres ->
                            if (loadedGenres.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    loadedGenres.forEach { genre ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                genre.name,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            } else {
                                DetailPlaceholder("genres", placeholderAlpha)
                            }
                        }
                    }
                    DetailValue(
                        "synopsis", movie?.synopsis, loadingMissingDetails, placeholderAlpha,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    DetailValue("status", movie?.status?.let { "Status: $it" }, loadingMissingDetails, placeholderAlpha)
                    DetailValue("original language", movie?.originalLanguage?.let { code ->
                        "Original language: ${languageName(code)}"
                    }, loadingMissingDetails, placeholderAlpha)
                }
                if (movie != null) trailerContent()
                if (viewState.isRefreshingDetails && movie?.detailsFetchedAt != null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = "Refreshing movie details"
                    })
                }
                val error = viewState.detailsError ?: viewState.errorMessage.takeIf { it.isNotBlank() }
                if (error != null) {
                    Column(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
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
    posterModifier: Modifier,
    movieId: Int?,
    onBack: (() -> Unit)?,
    onAddToList: (() -> Unit)?,
    loading: Boolean,
    placeholderAlpha: State<Float>,
    backdropModifier: Modifier,
    onImageTapped: ((MovieImageType) -> Unit)?,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val backdropHeight = 250.dp
        val posterWidth = (maxWidth / 3).coerceAtMost(120.dp)
        val posterHeight = posterWidth * 1.5f
        val overlap = posterHeight * 0.3f
        Box(Modifier.fillMaxWidth().height(backdropHeight)) {
            MoverImageView(
                posterUrl = movie?.backdropUrl.orEmpty(),
                contentDescription = "Movie backdrop image for ${movie?.title.orEmpty()}",
                sharedImageKey = movieId?.let(::movieBackdropKey),
                modifier = backdropModifier.fillMaxSize().then(
                    if (onImageTapped != null && !movie?.backdropUrl.isNullOrBlank()) {
                        Modifier.clickable(onClickLabel = "Expand backdrop", role = Role.Button) {
                            onImageTapped(MovieImageType.Backdrop)
                        }
                    } else Modifier,
                ),
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.6f)),
            )))
        }
        MovieSummary(
            movie, movieId, posterModifier, posterWidth, posterHeight, overlap,
            loading, placeholderAlpha,
            onImageTapped,
            modifier = Modifier.padding(top = backdropHeight - overlap),
        )
        Row(
            modifier = Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (onBack != null) {
                HeaderButton(Res.drawable.arrow_back, "Back", onClick = onBack)
            } else Spacer(Modifier.size(48.dp))
            if (movie != null && onAddToList != null) {
                HeaderButton(Res.drawable.add, "Add to list", onClick = onAddToList)
            }
        }
    }
}

@Composable
private fun HeaderButton(icon: DrawableResource, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)),
    ) {
        Icon(painterResource(icon), contentDescription = description, tint = Color.White)
    }
}

@Composable
private fun MovieSummary(
    movie: Movie?,
    movieId: Int?,
    posterModifier: Modifier,
    posterWidth: Dp,
    posterHeight: Dp,
    overlap: Dp,
    loading: Boolean,
    placeholderAlpha: State<Float>,
    onImageTapped: ((MovieImageType) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MoverImageView(
            posterUrl = movie?.posterUrl.orEmpty(),
            contentDescription = movie?.let { "Movie poster for ${it.title}" } ?: "Movie poster",
            sharedImageKey = movieId?.let(::moviePosterKey),
            modifier = posterModifier.align(Alignment.Bottom).size(width = posterWidth, height = posterHeight).then(
                if (onImageTapped != null && !movie?.posterUrl.isNullOrBlank()) {
                    Modifier.clickable(onClickLabel = "Expand poster", role = Role.Button) {
                        onImageTapped(MovieImageType.Poster)
                    }
                } else Modifier,
            ),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = posterHeight)
                .padding(top = overlap + 8.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        ) {
            val titleStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            DetailValue(
                "title", movie?.title, loading, placeholderAlpha,
                style = titleStyle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 18.sp,
                    maxFontSize = titleStyle.fontSize,
                    stepSize = 1.sp,
                ),
            )
            DetailValue("tagline", movie?.tagline, loading, placeholderAlpha)
            val releaseDate = movie?.releaseDate?.takeIf(String::isNotBlank)
            val runtime = movie?.runtimeMinutes?.let(::formatRuntime)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (releaseDate != null || runtime != null || loading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (releaseDate != null) {
                            Text(releaseDate, style = MaterialTheme.typography.bodyMedium)
                            if (runtime != null || loading) {
                                Text("•", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        DetailValue("runtime", runtime, loading, placeholderAlpha)
                    }
                }
                val score = movie?.voteAverage?.takeIf { it.isFinite() }?.coerceIn(0.0, 10.0)
                val votes = movie?.voteCount
                if (score != null && votes != null && votes > 0) {
                    MovieRating(score, votes)
                } else {
                    DetailValue(
                        "rating", formatRating(movie), loading, placeholderAlpha,
                        style = MaterialTheme.typography.bodySmall,
                    )
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
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    autoSize: TextAutoSize? = null,
) {
    val text = value?.takeIf { it.isNotBlank() }
    if (text == null && !loading) return
    AnimatedContent(
        targetState = text,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
        label = "Loaded $label",
    ) { loadedText ->
        if (loadedText != null) {
            Text(
                loadedText,
                modifier = modifier.fillMaxWidth(),
                style = style,
                maxLines = maxLines,
                overflow = overflow,
                autoSize = autoSize,
            )
        } else {
            DetailPlaceholder(label, placeholderAlpha)
        }
    }
}

@Composable
private fun DetailPlaceholder(label: String, opacity: State<Float>) {
    Spacer(
        modifier = Modifier.fillMaxWidth(0.65f).height(20.dp).clip(RoundedCornerShape(6.dp))
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
    else -> null
}

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
    DetailPreview(MovieDetailViewState(
        isLoading = false, isRefreshingDetails = true,
        movie = Movie(1, "Preview: The Movie", "A preview to die for...", "2026-10-07"),
    ))
}

@Preview(name = "Fetch failed", widthDp = 360, heightDp = 800)
@Composable
private fun FetchFailedPreview() {
    DetailPreview(MovieDetailViewState(
        isLoading = false, movie = previewMovie,
        detailsError = "Unable to load additional movie details.",
    ))
}

@Preview(name = "Long title · large text", widthDp = 320, heightDp = 800, fontScale = 1.5f)
@Composable
private fun LongTitlePreview() {
    DetailPreview(MovieDetailViewState(isLoading = false, movie = previewMovie.copy(
        title = "A Very Long Movie Title That Needs Room to Wrap",
        tagline = "A longer subtitle that should remain readable beside the film poster.",
    )))
}

@Preview(name = "Wide", widthDp = 840, heightDp = 640)
@Composable
private fun WidePreview() {
    DetailPreview(MovieDetailViewState(isLoading = false, movie = previewMovie))
}

@Composable
private fun DetailPreview(state: MovieDetailViewState) {
    MaterialTheme {
        MovieDetailScreen(onViewEvent = {}, viewState = state, onBack = {}, onAddToList = {})
    }
}

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.detail.domain.models.MovieGenre
import com.jcotters.designsystem.MoversIconButton
import com.jcotters.designsystem.MoversTextButton
import com.jcotters.details.navigation.MovieImageType
import com.jcotters.presentation.MoverImageView
import com.jcotters.presentation.movieBackdropKey
import com.jcotters.presentation.moviePosterKey
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.add
import com.jcotters.presentation.resources.arrow_back
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private const val MINUTES_PER_HOUR = 60

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
                    MovieGenres(movie?.genres.orEmpty(), loadingMissingDetails, placeholderAlpha)
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
                MovieDetailFeedback(viewState, loading, onRetry = { onViewEvent(MovieDetailViewEvent.RetryDetails) })
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
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.6f)),
                    )
                )
            )
        }
        MovieSummary(
            movie, movieId, posterModifier, posterWidth, posterHeight, overlap,
            loading, placeholderAlpha,
            onImageTapped,
            modifier = Modifier.padding(top = backdropHeight - overlap),
        )
        Row(
            modifier = Modifier.fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                )
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
    MoversIconButton(
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
            MovieReleaseAndRating(movie, loading, placeholderAlpha)
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
    minutes < MINUTES_PER_HOUR -> "$minutes min"
    minutes % MINUTES_PER_HOUR == 0 -> "${minutes / MINUTES_PER_HOUR}h"
    else -> "${minutes / MINUTES_PER_HOUR}h ${minutes % MINUTES_PER_HOUR}m"
}

internal fun formatRating(movie: Movie?): String? = when {
    movie?.voteCount == 0 -> "Not yet rated"
    else -> null
}

@Composable
private fun MovieGenres(genres: List<MovieGenre>, loading: Boolean, placeholderAlpha: State<Float>) {
    if (genres.isNotEmpty() || loading) {
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
}

@Composable
private fun MovieReleaseAndRating(movie: Movie?, loading: Boolean, placeholderAlpha: State<Float>) {
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

@Composable
private fun MovieDetailFeedback(viewState: MovieDetailViewState, loading: Boolean, onRetry: () -> Unit) {
    val error = viewState.detailsError ?: viewState.errorMessage.takeIf { it.isNotBlank() }
    if (error != null) {
        Column(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            MoversTextButton(
                onClick = onRetry,
                enabled = !loading
            ) {
                Text("Retry")
            }
        }
    } else if (viewState.movie == null && !loading) {
        Text("Movie details are unavailable.")
        MoversTextButton(onClick = onRetry) { Text("Retry") }
    }
}

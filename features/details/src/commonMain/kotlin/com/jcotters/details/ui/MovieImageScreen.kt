package com.jcotters.details.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jcotters.details.navigation.MovieImageType
import com.jcotters.presentation.ExpandedImageView
import com.jcotters.presentation.LocalMovieTransitionScope
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.close
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun MovieImageScreen(
    state: MovieImageViewState,
    imageType: MovieImageType,
    imageKey: String,
    onClose: () -> Unit,
    imageModifier: Modifier = Modifier,
) {
    val imageLabel = when (imageType) {
        MovieImageType.Poster -> "Movie poster"
        MovieImageType.Backdrop -> "Movie backdrop image"
    }
    val closeOverlay = LocalMovieTransitionScope.current?.let { scope ->
        with(scope) { Modifier.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f) }
    } ?: Modifier
    Box(Modifier.fillMaxSize().background(Color.Black).semantics { paneTitle = imageLabel }) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).then(imageModifier)) {
            val imageUrl = state.imageUrl
            when {
                imageUrl != null || state.isLoading -> ExpandedImageView(
                    imageUrl = imageUrl,
                    contentDescription = if (state.title.isBlank()) imageLabel else "$imageLabel for ${state.title}",
                    placeholderKey = imageKey,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> Text(
                    state.errorMessage ?: "Image unavailable.",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            }
        }
        Box(
            Modifier.align(Alignment.TopStart)
                .then(closeOverlay)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(12.dp),
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)),
            ) {
                Icon(painterResource(Res.drawable.close), contentDescription = "Close image", tint = Color.White)
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun MovieImagePreview() {
    MaterialTheme {
        MovieImageScreen(
            state = MovieImageViewState(isLoading = false, title = "Preview: The Movie", imageUrl = "preview"),
            imageType = MovieImageType.Poster,
            imageKey = "preview",
            onClose = {},
        )
    }
}

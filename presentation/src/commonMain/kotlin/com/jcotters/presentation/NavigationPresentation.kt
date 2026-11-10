package com.jcotters.presentation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalMovieTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun moviePosterTransitionModifier(movieId: Int, stackId: String): Modifier {
    val scope = LocalMovieTransitionScope.current ?: return Modifier
    val animatedScope = LocalNavAnimatedContentScope.current
    return with(scope) {
        Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(stackId to moviePosterKey(movieId)),
            animatedVisibilityScope = animatedScope,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun movieImageExpansionModifier(imageKey: String, sourceEntryId: String, stackId: String): Modifier {
    val scope = LocalMovieTransitionScope.current ?: return Modifier
    val animatedScope = LocalNavAnimatedContentScope.current
    return with(scope) {
        Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(listOf(stackId, sourceEntryId, imageKey)),
            animatedVisibilityScope = animatedScope,
            // The thumbnail is cropped, while the expanded artwork fits without cropping.
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Fit),
        )
    }
}

@Composable
fun ErrorMessageHandler(messages: Flow<String>) {
    val snackbar = LocalSnackbarHostState.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(messages, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            messages.collect { snackbar.showSnackbar(it) }
        }
    }
}

val LocalSnackbarHostState = staticCompositionLocalOf<androidx.compose.material3.SnackbarHostState> {
    error("No app snackbar host")
}

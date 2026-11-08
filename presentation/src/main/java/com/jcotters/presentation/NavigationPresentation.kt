package com.jcotters.presentation

import android.widget.Toast
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun ErrorMessageHandler(messages: Flow<String>) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(messages, lifecycle, context) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            messages.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        }
    }
}

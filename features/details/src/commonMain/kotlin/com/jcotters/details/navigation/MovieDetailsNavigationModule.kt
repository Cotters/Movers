package com.jcotters.details.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.NavDisplay
import com.jcotters.details.ui.MovieDetailScreen
import com.jcotters.details.ui.MovieDetailViewEvent
import com.jcotters.details.ui.MovieDetailViewModel
import com.jcotters.details.ui.MovieImageScreen
import com.jcotters.details.ui.MovieImageViewModel
import com.jcotters.details.ui.MovieTrailerSection
import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.presentation.movieBackdropKey
import com.jcotters.presentation.movieImageExpansionModifier
import com.jcotters.presentation.moviePosterKey
import com.jcotters.presentation.moviePosterTransitionModifier
import com.jcotters.watchlists.navigation.AddToListsKey
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val IMAGE_FADE_MILLIS = 250

object MovieDetailsNavigationModule {
    fun provideEntries(): EntryProviderInstaller = { navigator, stackId ->
        entry<MovieDetailsKey> { key ->
            val viewModel: MovieDetailViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(key.movieId) {
                viewModel.onViewEvent(MovieDetailViewEvent.OnLoad(key.movieId))
            }
            MovieDetailScreen(
                movieId = key.movieId,
                viewState = state,
                onViewEvent = viewModel::onViewEvent,
                posterModifier = moviePosterTransitionModifier(key.movieId, stackId).then(
                    movieImageExpansionModifier(moviePosterKey(key.movieId), key.instanceId, stackId),
                ),
                backdropModifier = movieImageExpansionModifier(movieBackdropKey(key.movieId), key.instanceId, stackId),
                onImageTapped = { imageType ->
                    navigator.navigate(
                        MovieImageKey(
                            key.movieId,
                            imageType,
                            key.instanceId
                        )
                    )
                },
                trailerContent = { MovieTrailerSection(key.movieId) },
                onBack = navigator::goBack,
                onAddToList = { navigator.navigate(AddToListsKey(key.movieId, key.targetListId)) },
            )
        }
        entry<MovieImageKey>(
            metadata = NavDisplay.transitionSpec { imageFade() } +
                NavDisplay.popTransitionSpec { imageFade() } +
                NavDisplay.predictivePopTransitionSpec { _ -> imageFade() },
        ) { key ->
            val viewModel: MovieImageViewModel = koinViewModel { parametersOf(key.movieId, key.imageType) }
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val imageKey = when (key.imageType) {
                MovieImageType.Poster -> moviePosterKey(key.movieId)
                MovieImageType.Backdrop -> movieBackdropKey(key.movieId)
            }
            MovieImageScreen(
                state = state,
                imageType = key.imageType,
                imageKey = imageKey,
                imageModifier = movieImageExpansionModifier(imageKey, key.sourceEntryId, stackId),
                onClose = navigator::goBack,
            )
        }
    }
}

private fun imageFade() = fadeIn(tween(IMAGE_FADE_MILLIS)) togetherWith fadeOut(tween(IMAGE_FADE_MILLIS))

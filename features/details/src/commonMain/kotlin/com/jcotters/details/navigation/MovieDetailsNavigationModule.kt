package com.jcotters.details.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.details.ui.MovieDetailScreen
import com.jcotters.details.ui.MovieDetailViewEvent
import com.jcotters.details.ui.MovieDetailViewModel
import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.presentation.moviePosterTransitionModifier
import org.koin.compose.viewmodel.koinViewModel
import com.jcotters.watchlists.navigation.AddToListsKey

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
                posterModifier = moviePosterTransitionModifier(key.movieId, stackId),
                onBack = navigator::goBack,
                onAddToList = { navigator.navigate(AddToListsKey(key.movieId, key.targetListId)) },
            )
        }
    }
}

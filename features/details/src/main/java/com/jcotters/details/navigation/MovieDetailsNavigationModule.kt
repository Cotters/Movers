package com.jcotters.details.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.details.ui.MovieDetailScreen
import com.jcotters.details.ui.MovieDetailViewEvent
import com.jcotters.details.ui.MovieDetailViewModel
import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.presentation.ErrorMessageHandler
import com.jcotters.presentation.moviePosterTransitionModifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object MovieDetailsNavigationModule {
    @Provides
    @IntoSet
    fun provideEntries(): EntryProviderInstaller = { navigator, stackId ->
        entry<MovieDetailsKey> { key ->
            val viewModel: MovieDetailViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ErrorMessageHandler(viewModel.errorMessage)
            LaunchedEffect(key.movieId) {
                viewModel.onViewEvent(MovieDetailViewEvent.OnLoad(key.movieId))
            }
            MovieDetailScreen(
                movieId = key.movieId,
                viewState = state,
                onViewEvent = viewModel::onViewEvent,
                posterModifier = moviePosterTransitionModifier(key.movieId, stackId),
            )
        }
    }
}

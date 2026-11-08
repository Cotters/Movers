package com.jcotters.profile.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.details.navigation.MovieDetailsKey
import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.presentation.moviePosterTransitionModifier
import com.jcotters.profile.ui.ProfileScreen
import com.jcotters.profile.ui.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

object ProfileNavigationModule {
    fun provideEntries(): EntryProviderInstaller = { navigator, stackId ->
        entry<ProfileKey> {
            val viewModel: ProfileViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ProfileScreen(
                viewState = state,
                onRetry = viewModel::retry,
                posterModifier = { movie -> moviePosterTransitionModifier(movie.id, stackId) },
                onMovieTapped = { navigator.navigate(MovieDetailsKey(it)) },
            )
        }
    }
}

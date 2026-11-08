package com.jcotters.profile.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.details.navigation.MovieDetailsKey
import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.presentation.ErrorMessageHandler
import com.jcotters.presentation.moviePosterTransitionModifier
import com.jcotters.profile.ui.ProfileScreen
import com.jcotters.profile.ui.ProfileViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object ProfileNavigationModule {
    @Provides
    @IntoSet
    fun provideEntries(): EntryProviderInstaller = { navigator, stackId ->
        entry<ProfileKey> {
            val viewModel: ProfileViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ErrorMessageHandler(viewModel.errorMessage)
            ProfileScreen(
                viewState = state,
                onViewEvent = viewModel::onViewEvent,
                posterModifier = { movie -> moviePosterTransitionModifier(movie.id, stackId) },
                onMovieTapped = { navigator.navigate(MovieDetailsKey(it)) },
            )
        }
    }
}

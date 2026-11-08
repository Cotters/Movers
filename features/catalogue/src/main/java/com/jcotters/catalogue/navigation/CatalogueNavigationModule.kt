package com.jcotters.catalogue.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.jcotters.catalogue.MovieCatalogueScreen
import com.jcotters.catalogue.MovieCatalogueViewModel
import com.jcotters.details.navigation.MovieDetailsKey
import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.presentation.moviePosterTransitionModifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object CatalogueNavigationModule {
    @Provides
    @IntoSet
    fun provideEntries(): EntryProviderInstaller = { navigator, stackId ->
        entry<CatalogueKey> {
            val viewModel: MovieCatalogueViewModel = hiltViewModel()
            MovieCatalogueScreen(
                movies = viewModel.popularMovies.collectAsLazyPagingItems(),
                posterModifier = { movie -> moviePosterTransitionModifier(movie.id, stackId) },
                onMovieTapped = { navigator.navigate(MovieDetailsKey(it)) },
            )
        }
    }
}

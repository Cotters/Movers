package com.jcotters.catalogue.navigation

import androidx.paging.compose.collectAsLazyPagingItems
import com.jcotters.catalogue.MovieCatalogueScreen
import com.jcotters.catalogue.MovieCatalogueViewModel
import com.jcotters.details.navigation.MovieDetailsKey
import com.jcotters.navigation.EntryProviderInstaller
import com.jcotters.presentation.moviePosterTransitionModifier
import org.koin.compose.viewmodel.koinViewModel

object CatalogueNavigationModule {
    fun provideEntries(): EntryProviderInstaller = { navigator, stackId ->
        entry<CatalogueKey> {
            val viewModel: MovieCatalogueViewModel = koinViewModel()
            MovieCatalogueScreen(
                movies = viewModel.popularMovies.collectAsLazyPagingItems(),
                posterModifier = { movie -> moviePosterTransitionModifier(movie.id, stackId) },
                onMovieTapped = { navigator.navigate(MovieDetailsKey(it)) },
            )
        }
    }
}

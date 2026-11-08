package com.jcotters.catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.jcotters.contract.catalogue.domain.IMovieCatalogueRepository

internal class MovieCatalogueViewModel(
    movieCatalogueRepository: IMovieCatalogueRepository,
) : ViewModel() {

    val popularMovies = movieCatalogueRepository
        .getPopularMoviesPaging()
        .cachedIn(viewModelScope)
}

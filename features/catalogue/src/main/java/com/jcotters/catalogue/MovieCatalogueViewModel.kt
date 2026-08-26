package com.jcotters.catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.jcotters.contract.catalogue.domain.IMovieCatalogueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MovieCatalogueViewModel @Inject constructor(
    movieCatalogueRepository: IMovieCatalogueRepository,
) : ViewModel() {

    val popularMovies = movieCatalogueRepository
        .getPopularMoviesPaging()
        .cachedIn(viewModelScope)
}
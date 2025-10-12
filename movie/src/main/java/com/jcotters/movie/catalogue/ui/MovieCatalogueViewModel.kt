package com.jcotters.movie.catalogue.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.jcotters.movie.catalogue.domain.GetPopularMoviesUseCase
import com.jcotters.movie.detail.domain.models.Movie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class MovieCatalogueViewState(
  val isLoading: Boolean = true,
  val movies: List<Movie> = emptyList()
)

@HiltViewModel
class MovieCatalogueViewModel @Inject constructor(
  getPopularMovies: GetPopularMoviesUseCase
) : ViewModel() {

  val popularMovies = getPopularMovies
    .popularMovies
    .cachedIn(viewModelScope)

  private val viewModelUiState = MutableStateFlow(MovieCatalogueViewState())
  val uiState: StateFlow<MovieCatalogueViewState> = viewModelUiState

}
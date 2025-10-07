package com.jcotters.movie.catalogue.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.movie.catalogue.domain.GetPopularMoviesUseCase
import com.jcotters.movie.detail.domain.models.Movie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovieCatalogueViewState(
  val isLoading: Boolean = true,
  val movies: List<Movie> = emptyList()
)

@HiltViewModel
class MovieCatalogueViewModel @Inject constructor(
  private val getPopularMovies: GetPopularMoviesUseCase
) : ViewModel() {

  private val viewModelUiState = MutableStateFlow(MovieCatalogueViewState())
  val uiState: StateFlow<MovieCatalogueViewState> = viewModelUiState

  init {
    viewModelScope.launch {
      getPopularMovies(page = 1)
        .let(::handleCatalogue)
    }
  }

  private fun handleCatalogue(movies: List<Movie>) {
    viewModelUiState.value = viewModelUiState.value.copy(
      isLoading = false,
      movies = movies,
    )
  }

}
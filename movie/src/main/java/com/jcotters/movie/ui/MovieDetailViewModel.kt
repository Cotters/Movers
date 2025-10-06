package com.jcotters.movie.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.movie.domain.GetMovieByIdUseCase
import com.jcotters.movie.domain.models.Movie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MovieDetailViewEvent {
  class OnLoad(val movieId: Int) : MovieDetailViewEvent
}

data class MovieDetailViewState(
  val isLoading: Boolean = true,
  val movie: Movie? = null,
  val errorMessage: String = "",
)

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
  private val getMovieById: GetMovieByIdUseCase,
) : ViewModel() {

  private val viewModelUiState = MutableStateFlow(MovieDetailViewState())
  val uiState: StateFlow<MovieDetailViewState> = viewModelUiState

  fun onViewEvent(event: MovieDetailViewEvent) {
    when (event) {
      is MovieDetailViewEvent.OnLoad -> onViewLoaded(event.movieId)
    }
  }

  private fun onViewLoaded(movieId: Int) {
    viewModelScope.launch {
      getMovieById(movieId)
        .onSuccess {
          viewModelUiState.emit(
            viewModelUiState.value.copy(
              isLoading = false,
              movie = it,
            )
          )
        }
        .onFailure {
          viewModelUiState.emit(
            viewModelUiState.value.copy(
              isLoading = false,
              errorMessage = it.message ?: "Failed to load mover.",
            )
          )
        }
    }
  }

}
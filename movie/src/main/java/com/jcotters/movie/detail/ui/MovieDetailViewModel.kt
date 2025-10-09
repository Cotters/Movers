package com.jcotters.movie.detail.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.movie.detail.domain.BookmarkMovieUseCase
import com.jcotters.movie.detail.domain.GetMovieByIdUseCase
import com.jcotters.movie.detail.domain.IsMovieBookmarkedUseCase
import com.jcotters.movie.detail.domain.models.Movie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
  private val getMovieById: GetMovieByIdUseCase,
  private val isMovieBookmarked: IsMovieBookmarkedUseCase,
  private val bookmarkMovieUseCase: BookmarkMovieUseCase,
) : ViewModel() {

  private val viewModelUiState = MutableStateFlow(MovieDetailViewState())
  val uiState: StateFlow<MovieDetailViewState> = viewModelUiState

  private val _errorMessage = MutableSharedFlow<String>()
  val errorMessage = _errorMessage.asSharedFlow()

  fun onViewEvent(event: MovieDetailViewEvent) {
    when (event) {
      is MovieDetailViewEvent.OnLoad -> onViewLoaded(event.movieId)
      is MovieDetailViewEvent.BookmarkTapped -> onBookmarkTapped(event.movieId)
    }
  }

  private fun onViewLoaded(movieId: Int) {
    viewModelScope.launch {
      isMovieBookmarked.invoke(movieId).let(::handleIsBookmarked)
      getMovieById.invoke(movieId).let(::handleMovieResult)
    }
  }

  private fun handleIsBookmarked(result: Result<Boolean>) {
    result.onSuccess {
      viewModelUiState.update { current ->
        current.copy(isBookmarked = it)
      }
    }
  }

  private fun handleMovieResult(result: Result<Movie>) {
    result
      .onSuccess { movie ->
        viewModelUiState.update { current ->
          current.copy(
            isLoading = false,
            movie = movie,
          )
        }
      }
      .onFailure { error ->
        viewModelUiState.update { current ->
          current.copy(isLoading = false)
        }
        emitError(error.message ?: "Failed to load mover.")
      }
  }

  private fun onBookmarkTapped(movieId: Int) {
    val isBookmarked = viewModelUiState.value.isBookmarked
    viewModelUiState.update { current ->
      current.copy(isBookmarked = current.isBookmarked.not())
    }
    viewModelScope.launch {
      if (isBookmarked) {
        bookmarkMovieUseCase.removeBookmark(movieId).let(::handleBookmarkResult)
      } else {
        bookmarkMovieUseCase.addBookmark(movieId).let(::handleBookmarkResult)
      }
    }
  }

  private fun handleBookmarkResult(result: Result<Unit>) {
    result
      .onFailure { error ->
        emitError(error.message ?: "Unable to bookmark film.")
        viewModelUiState.update { current ->
          current.copy(isBookmarked = current.isBookmarked.not())
        }
      }
  }

  private fun emitError(message: String) {
    viewModelScope.launch {
      _errorMessage.emit(message)
    }
  }

}
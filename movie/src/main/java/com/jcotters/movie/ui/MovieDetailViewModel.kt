package com.jcotters.movie.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.movie.domain.GetMovieByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MovieDetailViewEvent {
  class OnLoad(val movieId: Int) : MovieDetailViewEvent
}

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
  private val getMovieById: GetMovieByIdUseCase,
) : ViewModel() {

  fun onViewEvent(event: MovieDetailViewEvent) {
    when (event) {
      is MovieDetailViewEvent.OnLoad -> onViewLoaded(event.movieId)
    }
  }

  private fun onViewLoaded(movieId: Int) {
    viewModelScope.launch {
      getMovieById(movieId)
        .onSuccess {
          Log.d("TJ", "Success getting movie!! :)\n$it")
        }
        .onFailure {
          Log.d("TJ", "Failed getting movie!! :(\n$it")
        }
    }
  }

}
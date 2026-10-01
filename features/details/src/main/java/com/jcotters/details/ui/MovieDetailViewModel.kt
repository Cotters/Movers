package com.jcotters.details.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.details.domain.BookmarkMovieUseCase
import com.jcotters.details.domain.GetMovieByIdUseCase
import com.jcotters.details.domain.ObserveMovieBookmarkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val getMovieById: GetMovieByIdUseCase,
    private val observeMovieBookmark: ObserveMovieBookmarkUseCase,
    private val bookmarkMovieUseCase: BookmarkMovieUseCase,
) : ViewModel() {

    private val viewModelUiState = MutableStateFlow(MovieDetailViewState())
    val uiState: StateFlow<MovieDetailViewState> = viewModelUiState

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    private var bookmarkObservation: Job? = null
    private var loadedMovieId: Int? = null

    fun onViewEvent(event: MovieDetailViewEvent) {
        when (event) {
            is MovieDetailViewEvent.OnLoad -> onViewLoaded(event.movieId)
            is MovieDetailViewEvent.BookmarkTapped -> onBookmarkTapped(event.movieId)
        }
    }

    private fun onViewLoaded(movieId: Int) {
        if (loadedMovieId == movieId) return
        loadedMovieId = movieId
        bookmarkObservation?.cancel()
        viewModelUiState.value = MovieDetailViewState()
        bookmarkObservation = viewModelScope.launch {
            observeMovieBookmark(movieId)
                .catch { error -> emitError(error.message ?: "Unable to load bookmark.") }
                .collect { bookmarked ->
                    viewModelUiState.update { it.copy(isBookmarked = bookmarked, isBookmarkLoaded = true) }
                }
        }
        viewModelScope.launch {
            getMovieById.invoke(movieId).let(::handleMovieResult)
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
        val state = viewModelUiState.value
        if (state.isSavingBookmark || !state.isBookmarkLoaded || loadedMovieId != movieId) return
        viewModelUiState.update { it.copy(isSavingBookmark = true) }
        viewModelScope.launch {
            try {
                val result = if (state.isBookmarked) {
                    bookmarkMovieUseCase.removeBookmark(movieId)
                } else {
                    bookmarkMovieUseCase.addBookmark(movieId)
                }
                result.onSuccess {
                    val bookmarked = observeMovieBookmark(movieId).first()
                    viewModelUiState.update { it.copy(isBookmarked = bookmarked) }
                }.onFailure { error ->
                    emitError(error.message ?: "Unable to bookmark film.")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                emitError(error.message ?: "Unable to save bookmark.")
            } finally {
                viewModelUiState.update { it.copy(isSavingBookmark = false) }
            }
        }
    }

    private fun emitError(message: String) {
        viewModelScope.launch {
            _errorMessage.emit(message)
        }
    }

}
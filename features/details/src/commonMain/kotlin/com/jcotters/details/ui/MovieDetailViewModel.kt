package com.jcotters.details.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.contract.detail.domain.IMovieDetailsRepository
import com.jcotters.details.domain.BookmarkMovieUseCase
import com.jcotters.details.domain.ObserveMovieBookmarkUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class MovieDetailViewModel(
    private val movieDetailsRepository: IMovieDetailsRepository,
    private val observeMovieBookmark: ObserveMovieBookmarkUseCase,
    private val bookmarkMovieUseCase: BookmarkMovieUseCase,
) : ViewModel() {

    private val viewModelUiState = MutableStateFlow(MovieDetailViewState())
    val uiState: StateFlow<MovieDetailViewState> = viewModelUiState

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    private var bookmarkObservation: Job? = null
    private var movieObservation: Job? = null
    private var detailRefresh: Job? = null
    private var bookmarkWrite: Job? = null
    private var loadedMovieId: Int? = null

    fun onViewEvent(event: MovieDetailViewEvent) {
        when (event) {
            is MovieDetailViewEvent.OnLoad -> onViewLoaded(event.movieId)
            MovieDetailViewEvent.RetryDetails -> loadedMovieId?.let { movieId ->
                if (movieObservation?.isActive != true) observeMovie(movieId)
                refreshDetails(movieId)
            }
            is MovieDetailViewEvent.BookmarkTapped -> onBookmarkTapped(event.movieId)
        }
    }

    private fun onViewLoaded(movieId: Int) {
        if (loadedMovieId == movieId) return
        loadedMovieId = movieId
        movieObservation?.cancel()
        detailRefresh?.cancel()
        bookmarkObservation?.cancel()
        bookmarkWrite?.cancel()
        viewModelUiState.value = MovieDetailViewState()
        bookmarkObservation = viewModelScope.launch {
            observeMovieBookmark(movieId)
                .catch { error -> emitError(error.message ?: "Unable to load bookmark.") }
                .collect { bookmarked ->
                    viewModelUiState.update { it.copy(isBookmarked = bookmarked, isBookmarkLoaded = true) }
                }
        }
        observeMovie(movieId)
        refreshDetails(movieId)
    }

    private fun observeMovie(movieId: Int) {
        movieObservation = viewModelScope.launch {
            movieDetailsRepository.observeMovieWithId(movieId)
                .catch {
                    viewModelUiState.update { current ->
                        current.copy(isLoading = false, errorMessage = "Unable to read saved movie details.")
                    }
                }
                .collect { movie ->
                    if (loadedMovieId == movieId && movie != null) {
                        viewModelUiState.update { it.copy(movie = movie, isLoading = false, errorMessage = "") }
                    }
                }
        }
    }

    private fun refreshDetails(movieId: Int) {
        if (detailRefresh?.isActive == true) return
        viewModelUiState.update { it.copy(isRefreshingDetails = true, detailsError = null) }
        detailRefresh = viewModelScope.launch {
            try {
                movieDetailsRepository.refreshMovieWithId(movieId).onFailure {
                    viewModelUiState.update { current ->
                        current.copy(detailsError = "Unable to load additional movie details.")
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                viewModelUiState.update { it.copy(detailsError = "Unable to load additional movie details.") }
            } finally {
                if (loadedMovieId == movieId) {
                    viewModelUiState.update { current ->
                        current.copy(
                            // A successful refresh may finish before Room emits its first row.
                            isLoading = current.movie == null && current.detailsError == null && current.errorMessage.isBlank(),
                            isRefreshingDetails = false,
                        )
                    }
                }
            }
        }
    }

    private fun onBookmarkTapped(movieId: Int) {
        val state = viewModelUiState.value
        if (state.isSavingBookmark || !state.isBookmarkLoaded || loadedMovieId != movieId) return
        viewModelUiState.update { it.copy(isSavingBookmark = true) }
        bookmarkWrite = viewModelScope.launch {
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
                if (loadedMovieId == movieId) {
                    viewModelUiState.update { it.copy(isSavingBookmark = false) }
                }
            }
        }
    }

    private fun emitError(message: String) {
        viewModelScope.launch {
            _errorMessage.emit(message)
        }
    }

}

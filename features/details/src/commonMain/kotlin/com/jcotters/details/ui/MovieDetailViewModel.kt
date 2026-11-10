package com.jcotters.details.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.contract.detail.domain.IMovieDetailsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class MovieDetailViewModel(
    private val movieDetailsRepository: IMovieDetailsRepository,
) : ViewModel() {

    private val viewModelUiState = MutableStateFlow(MovieDetailViewState())
    val uiState: StateFlow<MovieDetailViewState> = viewModelUiState

    private var movieObservation: Job? = null
    private var detailRefresh: Job? = null
    private var loadedMovieId: Int? = null

    fun onViewEvent(event: MovieDetailViewEvent) {
        when (event) {
            is MovieDetailViewEvent.OnLoad -> onViewLoaded(event.movieId)
            MovieDetailViewEvent.RetryDetails -> loadedMovieId?.let { movieId ->
                if (movieObservation?.isActive != true) observeMovie(movieId)
                refreshDetails(movieId)
            }
        }
    }

    private fun onViewLoaded(movieId: Int) {
        if (loadedMovieId == movieId) return
        loadedMovieId = movieId
        movieObservation?.cancel()
        detailRefresh?.cancel()
        viewModelUiState.value = MovieDetailViewState()
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
}

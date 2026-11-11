package com.jcotters.details.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.contract.detail.domain.IMovieTrailersRepository
import com.jcotters.contract.detail.domain.MovieTrailer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class MovieTrailerState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val trailer: MovieTrailer? = null,
    val error: String? = null,
)

internal class MovieTrailerViewModel(
    private val movieId: Int,
    private val repository: IMovieTrailersRepository,
) : ViewModel() {
    private val state = MutableStateFlow(MovieTrailerState())
    val uiState = state.asStateFlow()

    fun load() {
        if (state.value.loading || state.value.loaded) return
        state.value = MovieTrailerState(loading = true)
        viewModelScope.launch {
            try {
                repository.trailerForMovie(movieId).fold(
                    onSuccess = { state.value = MovieTrailerState(loaded = true, trailer = it) },
                    onFailure = { state.value = MovieTrailerState(error = "Unable to find a trailer.") },
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.value = MovieTrailerState(error = "Unable to find a trailer.")
            }
        }
    }
}

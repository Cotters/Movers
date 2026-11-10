package com.jcotters.details.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcotters.contract.detail.domain.IMovieDetailsRepository
import com.jcotters.details.navigation.MovieImageType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal data class MovieImageViewState(
    val isLoading: Boolean = true,
    val title: String = "",
    val imageUrl: String? = null,
    val errorMessage: String? = null,
)

internal class MovieImageViewModel(
    movieId: Int,
    imageType: MovieImageType,
    movieDetailsRepository: IMovieDetailsRepository,
) : ViewModel() {
    val uiState = movieDetailsRepository.observeMovieWithId(movieId)
        .map { movie ->
            val imageUrl = when (imageType) {
                MovieImageType.Poster -> movie?.fullSizePosterUrl ?: movie?.posterUrl
                MovieImageType.Backdrop -> movie?.fullSizeBackdropUrl ?: movie?.backdropUrl
            }?.takeIf { it.isNotBlank() }
            MovieImageViewState(
                isLoading = false,
                title = movie?.title.orEmpty(),
                imageUrl = imageUrl,
                errorMessage = if (imageUrl == null) "Image unavailable." else null,
            )
        }
        .catch { error ->
            if (error is CancellationException || error !is Exception) throw error
            emit(MovieImageViewState(isLoading = false, errorMessage = "Unable to load image."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovieImageViewState())
}

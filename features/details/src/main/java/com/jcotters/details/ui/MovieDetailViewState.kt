package com.jcotters.details.ui

import com.jcotters.contract.detail.domain.models.Movie

data class MovieDetailViewState(
    val isLoading: Boolean = true,
    val movie: Movie? = null,
    val isBookmarked: Boolean = false,
    val errorMessage: String = "",
)

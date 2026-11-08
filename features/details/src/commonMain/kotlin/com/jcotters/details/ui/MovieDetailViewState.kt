package com.jcotters.details.ui

import com.jcotters.contract.detail.domain.models.Movie

data class MovieDetailViewState(
    val isLoading: Boolean = true,
    val movie: Movie? = null,
    val isRefreshingDetails: Boolean = false,
    val detailsError: String? = null,
    val isBookmarked: Boolean = false,
    val isBookmarkLoaded: Boolean = false,
    val isSavingBookmark: Boolean = false,
    val errorMessage: String = "",
)

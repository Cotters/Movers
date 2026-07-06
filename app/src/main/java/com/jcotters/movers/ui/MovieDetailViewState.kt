package com.jcotters.movers.ui

import com.jcotters.movie.detail.domain.models.Movie

data class MovieDetailViewState(
  val isLoading: Boolean = true,
  val movie: Movie? = null,
  val isBookmarked: Boolean = false,
  val errorMessage: String = "",
)

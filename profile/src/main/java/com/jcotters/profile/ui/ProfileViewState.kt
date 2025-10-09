package com.jcotters.profile.ui

import com.jcotters.movie.detail.domain.models.Movie

data class ProfileViewState(
  val isLoading: Boolean = true,
  val profile: Profile? = null,
  val bookmarkedMovies: List<Movie> = emptyList(),
)
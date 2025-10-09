package com.jcotters.profile.ui

import com.jcotters.movie.detail.domain.models.Movie
import com.jcotters.profile.domain.Profile

data class ProfileViewState(
  val isLoading: Boolean = true,
  val profile: Profile? = null,
  val bookmarkedMovies: List<Movie> = emptyList(),
)
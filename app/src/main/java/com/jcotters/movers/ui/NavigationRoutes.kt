package com.jcotters.movers.ui

import kotlinx.serialization.Serializable

sealed interface NavigationRoutes {
  @Serializable
  data object Catalogue

  @Serializable
  data class MovieDetails(val movieId: Int)

  @Serializable
  data object Login
}
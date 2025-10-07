package com.jcotters.movers.ui

import kotlinx.serialization.Serializable

sealed interface NavigationRoutes {

  @Serializable
  data object Auth
  @Serializable
  data object Login

  @Serializable
  data object Home

  @Serializable
  data object Catalogue

  @Serializable
  data class MovieDetails(val movieId: Int)
}
package com.jcotters.movers.ui

import kotlinx.serialization.Serializable

sealed interface NavigationRoutes {
  @Serializable
  data object Catalogue
}
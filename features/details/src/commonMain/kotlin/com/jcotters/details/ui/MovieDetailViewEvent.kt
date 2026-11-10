package com.jcotters.details.ui

sealed interface MovieDetailViewEvent {
  class OnLoad(val movieId: Int) : MovieDetailViewEvent
  data object RetryDetails : MovieDetailViewEvent
}

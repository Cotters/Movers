package com.jcotters.details.ui

sealed interface MovieDetailViewEvent {
  class OnLoad(val movieId: Int) : MovieDetailViewEvent
  class BookmarkTapped(val movieId: Int) : MovieDetailViewEvent
}

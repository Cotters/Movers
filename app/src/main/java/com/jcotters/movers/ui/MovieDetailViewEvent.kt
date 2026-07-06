package com.jcotters.movers.ui

sealed interface MovieDetailViewEvent {
  class OnLoad(val movieId: Int) : MovieDetailViewEvent
  class BookmarkTapped(val movieId: Int) : MovieDetailViewEvent
}

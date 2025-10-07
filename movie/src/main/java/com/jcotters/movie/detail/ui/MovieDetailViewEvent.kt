package com.jcotters.movie.detail.ui

sealed interface MovieDetailViewEvent {
  class OnLoad(val movieId: Int) : MovieDetailViewEvent
  class BookmarkTapped(val movieId: Int) : MovieDetailViewEvent
}

package com.jcotters.movie.detail.domain.models

data class Movie(
  val id: Int,
  val title: String,
  val synopsis: String,
  val releaseDate: String,
  val posterUrl: String? = null,
  val backdropUrl: String? = null,
)

object PreviewMovies {
  val movies: List<Movie> = listOf(
    Movie(id = 1, title = "Preview: The DbMovie", synopsis = "A thrilling preview!", releaseDate = "Today"),
    Movie(id = 2, title = "Preview: Reloaded", synopsis = "A thrilling hot reload!", releaseDate = "Tomorrow"),
    Movie(id = 3, title = "Preview: Recomp", synopsis = "A stunning recomposition!", releaseDate = "Friday"),
  )
}
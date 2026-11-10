package com.jcotters.contract.detail.domain.models

data class Movie(
  val id: Int,
  val title: String,
  val synopsis: String,
  val releaseDate: String,
  val posterUrl: String? = null,
  val backdropUrl: String? = null,
  val tagline: String? = null,
  val runtimeMinutes: Int? = null,
  val genres: List<MovieGenre> = emptyList(),
  val voteAverage: Double? = null,
  val voteCount: Int? = null,
  val status: String? = null,
  val originalLanguage: String? = null,
  val detailsFetchedAt: Long? = null,
  val fullSizePosterUrl: String? = null,
  val fullSizeBackdropUrl: String? = null,
)

data class MovieGenre(val id: Int, val name: String)

object PreviewMovies {
  val movies: List<Movie> = listOf(
    Movie(id = 1, title = "Preview: The DbMovie", synopsis = "An enticing preview!", releaseDate = "Today"),
    Movie(id = 2, title = "Preview: Reloaded", synopsis = "A thrilling hot reload!", releaseDate = "Tomorrow"),
    Movie(id = 3, title = "Preview: Recomp", synopsis = "A stunning recomposition!", releaseDate = "Friday"),
  )
}

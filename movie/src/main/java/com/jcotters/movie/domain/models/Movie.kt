package com.jcotters.movie.domain.models

data class Movie(
  val id: Int,
  val title: String,
  val releaseDate: String,
  val synopsis: String,
)

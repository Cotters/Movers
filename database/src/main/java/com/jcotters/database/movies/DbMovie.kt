package com.jcotters.database.movies

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "movies")
@TypeConverters(MovieGenreConverters::class)
data class DbMovie(
  @PrimaryKey
  val id: Int,
  val title: String,
  val synopsis: String,
  val releaseDate: String,
  val posterUrl: String? = null,
  val backdropUrl: String? = null,
  val tagline: String? = null,
  val runtimeMinutes: Int? = null,
  val genres: List<DbMovieGenre>? = null,
  val voteAverage: Double? = null,
  val voteCount: Int? = null,
  val status: String? = null,
  val originalLanguage: String? = null,
  val detailsFetchedAt: Long? = null,
)

package com.jcotters.database.movies

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class Movie(
  @PrimaryKey
  val id: Int,
  val title: String,
  val synopsis: String,
  val releaseDate: String,
  val posterUrl: String? = null,
  val backdropUrl: String? = null,
)
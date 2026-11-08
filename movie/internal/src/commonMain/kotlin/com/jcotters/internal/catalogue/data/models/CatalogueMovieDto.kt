package com.jcotters.internal.catalogue.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CatalogueMovieDto(
  @SerialName("id")
  val id: Int? = null,
  @SerialName("title")
  val title: String? = null,
  @SerialName("overview")
  val overview: String? = null,
  @SerialName("release_date")
  val releaseDate: String? = null,
  @SerialName("original_title")
  val originalTitle: String? = null,
  @SerialName("backdrop_path")
  val backdropPath: String? = null,
  @SerialName("original_language")
  val originalLanguage: String? = null,
  @SerialName("poster_path")
  val posterPath: String? = null,
  @SerialName("video")
  val video: Boolean? = null,
  @SerialName("vote_average")
  val voteAverage: Double? = null,
  @SerialName("vote_count")
  val voteCount: Int? = null,
)

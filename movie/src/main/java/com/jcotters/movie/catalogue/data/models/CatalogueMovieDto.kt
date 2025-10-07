package com.jcotters.movie.catalogue.data.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class CatalogueMovieDto(
  @SerializedName("id")
  val id: Int? = null,
  @SerializedName("title")
  val title: String? = null,
  @SerializedName("overview")
  val overview: String? = null,
  @SerializedName("release_date")
  val releaseDate: String? = null,
  @SerializedName("original_title")
  val originalTitle: String? = null,
  @SerializedName("backdrop_path")
  val backdropPath: String? = null,
  @SerializedName("original_language")
  val originalLanguage: String? = null,
  @SerializedName("poster_path")
  val posterPath: String? = null,
  @SerializedName("video")
  val video: Boolean? = null,
)
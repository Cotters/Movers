package com.jcotters.movie.detail.data.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class BelongsToCollection(
  @SerializedName("backdrop_path")
  val backdropPath: String? = null,
  @SerializedName("id")
  val id: Int? = null,
  @SerializedName("name")
  val name: String? = null,
  @SerializedName("poster_path")
  val posterPath: String? = null
)
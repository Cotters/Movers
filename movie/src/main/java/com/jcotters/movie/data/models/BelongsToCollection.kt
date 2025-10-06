package com.jcotters.movie.data.models

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class BelongsToCollection(
  @SerialName("id")
  val id: Int? = null,
  @SerialName("name")
  val name: String? = null,
  @SerialName("backdrop_path")
  val backdropPath: String? = null,
  @SerialName("poster_path")
  val posterPath: String? = null
)
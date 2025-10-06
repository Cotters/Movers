package com.jcotters.movie.data.models

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Genre(
  @SerialName("id")
  val id: Int? = null,
  @SerialName("name")
  val name: String? = null
)
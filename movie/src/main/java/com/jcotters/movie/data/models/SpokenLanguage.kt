package com.jcotters.movie.data.models

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class SpokenLanguage(
  @SerialName("name")
  val name: String? = null,
  @SerialName("english_name")
  val englishName: String? = null,
  @SerialName("iso_639_1")
  val iso6391: String? = null,
)
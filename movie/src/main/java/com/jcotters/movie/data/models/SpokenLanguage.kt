package com.jcotters.movie.data.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class SpokenLanguage(
  @SerializedName("english_name")
  val englishName: String? = null,
  @SerializedName("iso_639_1")
  val iso6391: String? = null,
  @SerializedName("name")
  val name: String? = null
)
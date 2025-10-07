package com.jcotters.movie.detail.data.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class Genre(
  @SerializedName("id")
  val id: Int? = null,
  @SerializedName("name")
  val name: String? = null
)
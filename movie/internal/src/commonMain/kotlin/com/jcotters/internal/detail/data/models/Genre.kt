package com.jcotters.internal.detail.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Genre(
  @SerialName("id")
  val id: Int? = null,
  @SerialName("name")
  val name: String? = null
)

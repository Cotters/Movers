package com.jcotters.internal.detail.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductionCompany(
  @SerialName("id")
  val id: Int? = null,
  @SerialName("logo_path")
  val logoPath: String? = null,
  @SerialName("name")
  val name: String? = null,
  @SerialName("origin_country")
  val originCountry: String? = null
)

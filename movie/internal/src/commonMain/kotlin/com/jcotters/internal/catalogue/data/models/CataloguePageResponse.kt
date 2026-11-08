package com.jcotters.internal.catalogue.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CataloguePageResponse(
  @SerialName("page")
  val page: Int? = null,
  @SerialName("results")
  val results: List<CatalogueMovieDto?>? = emptyList(),
  @SerialName("total_pages")
  val totalPages: Int? = null,
  @SerialName("total_results")
  val totalResults: Int? = null,
)

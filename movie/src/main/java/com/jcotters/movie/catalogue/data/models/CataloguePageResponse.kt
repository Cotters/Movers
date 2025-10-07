package com.jcotters.movie.catalogue.data.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class CataloguePageResponse(
  @SerializedName("page")
  val page: Int? = null,
  @SerializedName("results")
  val results: List<CatalogueMovieDto?>? = emptyList(),
  @SerializedName("total_pages")
  val totalPages: Int? = null,
  @SerializedName("total_results")
  val totalResults: Int? = null,
)

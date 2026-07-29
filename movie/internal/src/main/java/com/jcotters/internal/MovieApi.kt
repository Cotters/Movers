package com.jcotters.internal

import com.jcotters.internal.catalogue.data.models.CataloguePageResponse
import com.jcotters.internal.detail.data.models.MovieDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieApi {
  @GET("movie/{id}")
  suspend fun getMovieById(@Path("id") movieId: Int): MovieDto

  @GET("discover/movie")
  suspend fun getPopularMovies(
      @Query("page") page: Int,
      @Query("sort_by") sortBy: String = "popularity.desc",
  ): CataloguePageResponse
}

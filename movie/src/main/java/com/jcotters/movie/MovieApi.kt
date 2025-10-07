package com.jcotters.movie

import com.jcotters.movie.detail.data.models.MovieDto
import retrofit2.http.GET
import retrofit2.http.Path

interface MovieApi {
  @GET("movie/{id}")
  suspend fun getMovieById(@Path("id") movieId: Int): MovieDto
}
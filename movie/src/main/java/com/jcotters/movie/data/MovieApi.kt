package com.jcotters.movie.data

import com.jcotters.movie.data.models.MovieDto
import retrofit2.http.GET

interface MovieApi {
  @GET("movie/{id}")
  fun getMovieDetails(movieId: Int): MovieDto
}
package com.jcotters

import com.jcotters.movie.data.MovieApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object MovieModule {

  const val API_VERSION = "3"
  const val API_URL = "https://api.themoviedb.org/$API_VERSION/"

  @Provides
  fun provideRetrofit(): Retrofit {
    return Retrofit.Builder()
      .baseUrl(API_URL)
      .build()
  }

  @Provides
  fun provideMovieApi(retrofit: Retrofit): MovieApi {
    return retrofit.create(MovieApi::class.java)
  }

}
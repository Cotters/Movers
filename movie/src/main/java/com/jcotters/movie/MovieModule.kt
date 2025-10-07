package com.jcotters.movie

import com.jcotters.movie.detail.data.MovieDetailsRepository
import com.jcotters.movie.detail.domain.IMovieDetailsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object MovieModule {

  const val API_VERSION = "3"
  const val API_URL = "https://api.themoviedb.org/$API_VERSION/"
  const val API_TOKEN = "YOUR_API_KEY"

  @Provides
  fun provideOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
      .authenticator(object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
          val requestBuilder = response.request.newBuilder()
          requestBuilder.header("Authorization", "Bearer $API_TOKEN")
          return requestBuilder.build()
        }
      })
      .build()
  }

  @Provides
  fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
    return Retrofit.Builder()
      .baseUrl(API_URL)
      .client(okHttpClient)
      .addConverterFactory(GsonConverterFactory.create())
      .build()
  }

  @Provides
  fun provideMovieApi(retrofit: Retrofit): MovieApi {
    return retrofit.create(MovieApi::class.java)
  }

  @Provides
  fun provideMovieDetailsRepository(
    impl: MovieDetailsRepository
  ): IMovieDetailsRepository = impl

}
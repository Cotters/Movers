package com.jcotters.movie

import com.jcotters.movie.catalogue.data.MovieCatalogueRepository
import com.jcotters.movie.catalogue.domain.IMovieCatalogueRepository
import com.jcotters.movie.detail.data.BookmarksRepository
import com.jcotters.movie.detail.data.MovieDetailsRepository
import com.jcotters.movie.detail.domain.IBookmarksRepository
import com.jcotters.movie.detail.domain.IMovieDetailsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
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
      .addInterceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        requestBuilder.header("Authorization", "Bearer $API_TOKEN")
        val response = chain.proceed(requestBuilder.build())
        response
      }
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

  @Provides
  fun provideMovieCatalogueRepository(
    impl: MovieCatalogueRepository
  ): IMovieCatalogueRepository = impl

  @Provides
  fun provideBookmarksRepository(
    impl: BookmarksRepository,
  ): IBookmarksRepository = impl
}
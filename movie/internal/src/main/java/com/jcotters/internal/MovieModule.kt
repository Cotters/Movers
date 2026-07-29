package com.jcotters.internal

import com.jcotters.contract.catalogue.domain.IMovieCatalogueRepository
import com.jcotters.contract.detail.domain.IBookmarksRepository
import com.jcotters.contract.detail.domain.IMovieDetailsRepository
import com.jcotters.internal.catalogue.data.MovieCatalogueRepository
import com.jcotters.internal.detail.data.BookmarksRepository
import com.jcotters.internal.detail.data.MovieDetailsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
internal object MovieModule {

    const val API_VERSION = "3"
    const val API_URL = "https://api.themoviedb.org/$API_VERSION/"
    const val API_TOKEN = "PASTE YOUR OWN TOKEN HERE"

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
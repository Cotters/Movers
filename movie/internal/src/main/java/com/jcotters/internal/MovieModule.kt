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
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object MovieModule {

    const val API_VERSION = "3"
    const val API_URL = "https://api.themoviedb.org/$API_VERSION/"
    const val API_TOKEN = ApiConstants.API_KEY // Use your own key.

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor()
                    .apply {
//                        if (isDebug) {
                            setLevel(HttpLoggingInterceptor.Level.BODY)
//                        }
                    },
            )
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                requestBuilder.header("Authorization", "Bearer $API_TOKEN")
                chain.proceed(requestBuilder.build())
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(API_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideMovieApi(retrofit: Retrofit): MovieApi {
        return retrofit.create(MovieApi::class.java)
    }

    @Provides
    fun provideMovieDetailsRepository(
        impl: MovieDetailsRepository
    ): IMovieDetailsRepository = impl

    @Provides
    @Singleton
    fun provideMovieCatalogueRepository(
        impl: MovieCatalogueRepository
    ): IMovieCatalogueRepository = impl

    @Provides
    fun provideBookmarksRepository(
        impl: BookmarksRepository,
    ): IBookmarksRepository = impl
}
package com.jcotters.internal

import com.jcotters.internal.catalogue.data.models.CataloguePageResponse
import com.jcotters.internal.detail.data.models.MovieDto
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

interface MovieApi {
    suspend fun getMovieById(movieId: Int): MovieDto
    suspend fun getPopularMovies(page: Int, sortBy: String = "popularity.desc"): CataloguePageResponse
}

class KtorMovieApi(private val client: HttpClient) : MovieApi {
    override suspend fun getMovieById(movieId: Int): MovieDto = client.get("movie/$movieId").body()
    override suspend fun getPopularMovies(page: Int, sortBy: String): CataloguePageResponse =
        client.get("discover/movie") { parameter("page", page); parameter("sort_by", sortBy) }.body()
}

fun HttpClientConfig<*>.configureMovieApi(token: String) {
    expectSuccess = true
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    defaultRequest {
        url("https://api.themoviedb.org/3/")
        bearerAuth(token)
    }
}

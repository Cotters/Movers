package com.jcotters.internal

import com.jcotters.internal.catalogue.data.models.CatalogueMovieDto
import com.jcotters.internal.catalogue.data.models.CataloguePageResponse
import com.jcotters.internal.detail.data.MovieVideosResponse
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ExternalMovieResponse(@SerialName("movie_results") val movies: List<CatalogueMovieDto?> = emptyList())

interface MovieApi {
    suspend fun getMovieById(movieId: Int): MovieDto
    suspend fun getMovieVideos(movieId: Int): MovieVideosResponse
    suspend fun getPopularMovies(page: Int, sortBy: String = "popularity.desc"): CataloguePageResponse
    suspend fun searchMovies(query: String, page: Int, year: Int? = null): CataloguePageResponse
    suspend fun findMovie(imdbId: String): ExternalMovieResponse
}

class KtorMovieApi(private val client: HttpClient) : MovieApi {
    override suspend fun searchMovies(query: String, page: Int, year: Int?): CataloguePageResponse = client.get("search/movie") {
        parameter("query", query)
        parameter("page", page)
        parameter("include_adult", false)
        year?.let { parameter("primary_release_year", it) }
    }.body()
    override suspend fun findMovie(imdbId: String): ExternalMovieResponse = client.get("find/$imdbId") {
        parameter("external_source", "imdb_id")
    }.body()
    override suspend fun getMovieVideos(movieId: Int): MovieVideosResponse = client.get("movie/$movieId/videos") {
        parameter("language", "en-US")
    }.body()
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

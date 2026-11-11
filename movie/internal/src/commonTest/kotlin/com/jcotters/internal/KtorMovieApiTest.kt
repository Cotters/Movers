package com.jcotters.internal

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KtorMovieApiTest {
    @Test
    fun searchEncodesQueryAndPreservesPagingAndAdultFilter() = runTest {
        val client = HttpClient(MockEngine { request ->
            assertEquals("/3/search/movie", request.url.encodedPath)
            assertEquals("L'été & the city", request.url.parameters["query"])
            assertEquals("2", request.url.parameters["page"])
            assertEquals("false", request.url.parameters["include_adult"])
            assertEquals("2026", request.url.parameters["primary_release_year"])
            assertEquals("Bearer test-token", request.headers[HttpHeaders.Authorization])
            respond("""{"page":2,"total_pages":2,"results":[]}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) { configureMovieApi("test-token") }
        try { assertEquals(2, KtorMovieApi(client).searchMovies("L'été & the city", 2, 2026).page) } finally { client.close() }
    }

    @Test
    fun detailsUseBearerAuthenticationAndDecodeNullableFields() = runTest {
        val engine = MockEngine { request ->
            assertEquals("https://api.themoviedb.org/3/movie/12", request.url.toString())
            assertEquals("Bearer test-token", request.headers[HttpHeaders.Authorization])
            respond(
                """{"id":12,"title":"Sample","poster_path":null,"revenue":3000000000,"future_field":true}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) { configureMovieApi("test-token") }
        try {
            val movie = KtorMovieApi(client).getMovieById(12)
            assertEquals(12, movie.id)
            assertEquals("Sample", movie.title)
            assertNull(movie.posterPath)
            assertEquals(3_000_000_000L, movie.revenue)
        } finally { client.close() }
    }

    @Test
    fun cataloguePreservesPageAndSortParameters() = runTest {
        val client = HttpClient(MockEngine { request ->
            assertEquals("/3/discover/movie", request.url.encodedPath)
            assertEquals("2", request.url.parameters["page"])
            assertEquals("popularity.desc", request.url.parameters["sort_by"])
            respond(
                """{"page":2,"total_pages":3,"results":[]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) { configureMovieApi("test-token") }
        try {
            val page = KtorMovieApi(client).getPopularMovies(2)
            assertEquals(2, page.page)
            assertEquals(3, page.totalPages)
            assertTrue(page.results.orEmpty().isEmpty())
        } finally { client.close() }
    }

    @Test
    fun serverFailuresRemainFailures() = runTest {
        val client = HttpClient(MockEngine { respond("Unavailable", HttpStatusCode.ServiceUnavailable) }) {
            configureMovieApi("test-token")
        }
        try {
            assertFailsWith<ServerResponseException> { KtorMovieApi(client).getMovieById(12) }
        } finally { client.close() }
    }
}

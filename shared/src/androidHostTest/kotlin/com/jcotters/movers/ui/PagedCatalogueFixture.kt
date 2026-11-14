package com.jcotters.movers.ui

import com.jcotters.database.MoversDatabase
import com.jcotters.database.movies.DbMovie
import com.jcotters.database.movies.DbMovieCatalogueEntry
import com.jcotters.database.movies.DbMovieCatalogueRemoteKey
import com.jcotters.database.movies.MovieCatalogue
import io.ktor.http.Url
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

/** Real 20-film pages; only transport responses and their completion are controlled by tests. */
internal class PagedCatalogueFixture(
    private val cachedPages: Int = 15,
    private val totalPages: Int = cachedPages,
    pausedPages: Set<Int> = emptySet(),
) {
    private val gates = pausedPages.associateWith { CompletableDeferred<Unit>() }
    private val requests = ConcurrentHashMap.newKeySet<Int>()

    suspend fun seed(database: MoversDatabase) {
        val movies = (1..cachedPages * PAGE_SIZE).map(::movie)
        database.movieDao().insertMovies(movies)
        database.movieCatalogueDao().insertCatalogueEntries(movies.map { movie ->
            DbMovieCatalogueEntry(CATALOGUE, movie.id, (movie.id - 1) / PAGE_SIZE + 1, (movie.id - 1) % PAGE_SIZE)
        })
        database.movieCatalogueRemoteKeysDao().insertKey(
            DbMovieCatalogueRemoteKey(
                CATALOGUE, (cachedPages + 1).takeIf { it <= totalPages }, System.currentTimeMillis(),
            )
        )
    }

    suspend fun respond(url: Url): String? = when {
        url.encodedPath == "/3/discover/movie" -> {
            val page = requireNotNull(url.parameters["page"]).toInt()
            requests.add(page)
            gates[page]?.await()
            val results = ((page - 1) * PAGE_SIZE + 1..page * PAGE_SIZE).joinToString(",") { jsonMovie(it) }
            """{"page":$page,"total_pages":$totalPages,"results":[$results]}"""
        }

        url.encodedPath.endsWith("/videos") -> """{"results":[]}"""
        url.encodedPath.startsWith("/3/movie/") -> {
            val id = url.encodedPath.substringAfterLast('/').toInt()
            jsonMovie(id, details = true)
        }

        else -> null
    }

    fun requested(page: Int): Boolean = page in requests

    fun release(page: Int) {
        gates.getValue(page).complete(Unit)
    }

    val requestedPages: Set<Int> get() = requests.toSet()

    companion object {
        const val PAGE_SIZE = 20
        private val CATALOGUE = MovieCatalogue.Popular.key

        fun title(id: Int, revision: Int = 0): String = "Film ${id.toString().padStart(3, '0')} v$revision"

        fun movie(id: Int): DbMovie = DbMovie(id, title(id), "Catalogue synopsis", "2026-01-01")

        private fun jsonMovie(id: Int, details: Boolean = false): String {
            val enrichment = if (details) """, "runtime":125,"tagline":"Enriched film $id"""" else ""
            return """{"id":$id,"title":"${title(id)}","overview":"Catalogue synopsis",""" +
                """"release_date":"2026-01-01"$enrichment}"""
        }
    }
}

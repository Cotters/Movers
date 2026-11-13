package com.jcotters.internal.watchlists

import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.time.AppClock
import com.jcotters.contract.watchlists.data.FilmReference
import com.jcotters.contract.watchlists.data.FilmResolution
import com.jcotters.contract.watchlists.data.MovieImportResolver
import com.jcotters.contract.watchlists.data.MovieLookupException
import com.jcotters.database.movies.MovieDao
import com.jcotters.internal.MovieApi
import com.jcotters.internal.catalogue.data.models.CatalogueMovieDto
import com.jcotters.internal.detail.data.MovieMapper
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException

private const val MILLIS_PER_SECOND = 1000L
private const val MAX_RETRY_MILLIS = 30_000L
private const val DEFAULT_RETRY_MILLIS = 2000L

internal class TmdbMovieImportResolver(
    private val api: MovieApi,
    private val dao: MovieDao,
    private val mapper: MovieMapper,
    private val clock: AppClock,
) : MovieImportResolver {
    override suspend fun resolve(reference: FilmReference): FilmResolution = lookup {
        val byId = reference.movieId?.let { id ->
            dao.getMovieById(id)?.let(mapper::toDomainModel) ?: try {
                api.getMovieById(id).let { dto ->
                    mapper.toDatabaseModel(dto, clock.millis())?.takeIf { it.id == id }?.let { row ->
                        dao.saveMovieDetails(row)
                        dao.getMovieById(id)?.let(mapper::toDomainModel)
                    }
                }
            } catch (error: ClientRequestException) {
                if (error.response.status == HttpStatusCode.NotFound) null else throw error
            }
        }
        val byImdb = reference.imdbId?.let { imdbId ->
            try {
                save(api.findMovie(imdbId).movies).firstOrNull()
            } catch (error: ClientRequestException) {
                if (error.response.status == HttpStatusCode.NotFound) null else throw error
            }
        }
        if (reference.movieId != null || reference.imdbId != null) {
            if (reference.movieId != null && reference.imdbId != null && (byId == null || byImdb == null || byId.id != byImdb.id)) {
                return@lookup FilmResolution.Review(
                    listOfNotNull(byId, byImdb).distinctBy { it.id },
                    "Movie IDs don't agree."
                )
            }
            (byId ?: byImdb)?.let { return@lookup FilmResolution.Matched(it) }
        }
        if (reference.title.isBlank()) return@lookup FilmResolution.Review(emptyList(), "No film found for this ID.")
        var candidates = save(api.searchMovies(reference.title, 1, reference.year).results.orEmpty())
        if (candidates.isEmpty() && reference.year != null) candidates =
            save(api.searchMovies(reference.title, 1).results.orEmpty())
        candidates.firstOrNull()?.let { FilmResolution.Matched(it) }
            ?: FilmResolution.Review(emptyList(), "No matching film.")
    }

    override suspend fun search(query: String): List<Movie> = lookup {
        val text = query.trim()
        if (text.isBlank()) emptyList() else save(api.searchMovies(text, 1).results.orEmpty())
    }

    private suspend fun save(results: List<CatalogueMovieDto?>): List<Movie> {
        val rows =
            mapper.toDatabaseModel(results.filterNotNull().filter { (it.id ?: 0) > 0 && !it.title.isNullOrBlank() }
                .map { it.copy(overview = it.overview.orEmpty()) })
        dao.upsertCatalogueMovies(rows)
        return rows.map { row -> mapper.toDomainModel(dao.getMovieById(row.id) ?: row) }.distinctBy { it.id }
    }

    private suspend fun <T> lookup(block: suspend () -> T): T = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: ClientRequestException) {
        if (error.response.status == HttpStatusCode.TooManyRequests) {
            val wait = error.response.headers["Retry-After"]?.toLongOrNull()?.times(MILLIS_PER_SECOND)
                ?.coerceIn(MILLIS_PER_SECOND, MAX_RETRY_MILLIS)
            throw MovieLookupException("Movie search is busy. Retry shortly.", wait ?: DEFAULT_RETRY_MILLIS)
        }
        throw MovieLookupException("Unable to look up films. Retry when connected.")
    } catch (_: Exception) {
        throw MovieLookupException("Unable to look up films. Retry when connected.")
    }
}

package com.jcotters.contract.watchlists.data

import com.jcotters.contract.detail.domain.models.Movie
import kotlinx.serialization.Serializable

const val MAX_IMPORT_BATCH_SIZE = 50

data class FilmReference(
    val title: String = "",
    val year: Int? = null,
    val movieId: Int? = null,
    val imdbId: String? = null,
)

sealed interface FilmResolution {
    data class Matched(val movie: Movie) : FilmResolution
    data class Review(val candidates: List<Movie>, val reason: String) : FilmResolution
}

interface MovieImportResolver {
    suspend fun resolve(reference: FilmReference): FilmResolution
    suspend fun search(query: String): List<Movie>
}

@Serializable
enum class ImportDisposition { Imported, AlreadyPresent, Rejected }

@Serializable
data class FilmImportOutcome(val movieId: Int, val disposition: ImportDisposition)

@Serializable
data class ImportBatchResult(val outcomes: List<FilmImportOutcome>, val revision: Long? = null)

class ImportUnavailableException : IllegalStateException("Import isn't available on this server yet.")
class MovieLookupException(message: String, val retryAfterMillis: Long? = null) : IllegalStateException(message)

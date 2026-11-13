package com.jcotters.internal.watchlists

import com.jcotters.contract.result.recoverableResult
import com.jcotters.contract.time.AppClock
import com.jcotters.contract.watchlists.data.MAX_IMPORT_BATCH_SIZE
import com.jcotters.contract.watchlists.data.WatchlistDatabase
import com.jcotters.contract.watchlists.data.WatchlistRecord
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import com.jcotters.contract.watchlists.domain.Watchlist
import com.jcotters.contract.watchlists.domain.WatchlistContent
import com.jcotters.contract.watchlists.domain.WatchlistNotFoundException
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.map

internal class WatchlistsRepository(private val database: WatchlistDatabase, private val clock: AppClock) :
    IWatchlistsRepository {
    override fun observeWatchlists() = database.observeLists().map { rows -> rows.map(WatchlistRecord::toDomain) }
    override fun observeWatchlist(id: String) = database.observeContent(id)
        .map { it?.let { content -> WatchlistContent(content.list.toDomain(), content.movies, content.contributions) } }

    override fun observeMovieIds(listId: String) = database.observeMovieIds(listId)
    override fun observeListIds(movieId: Int) = database.observeListIds(movieId)

    override suspend fun create(name: String): Result<String> = write("Unable to create the list.") {
        val validName = validateName(name)
        val id = Uuid.random().toString()
        database.create(id, validName, clock.millis())
        id
    }

    override suspend fun rename(id: String, name: String) =
        write("Unable to rename the list.") { database.rename(id, validateName(name)) }

    override suspend fun delete(id: String) = write("Unable to delete the list.") { database.delete(id) }
    override suspend fun addMovieToLists(movieId: Int, listIds: Set<String>) =
        write("Unable to add this movie to the selected lists.") {
            require(listIds.isNotEmpty()) { "Choose at least one list." }
            database.addMovieToLists(movieId, listIds, clock.millis())
        }

    override suspend fun removeMovie(listId: String, movieId: Int) =
        write("Unable to remove this movie from the list.") { database.removeMovie(listId, movieId) }

    override suspend fun importMovies(
        requestId: String,
        listId: String,
        films: List<com.jcotters.contract.watchlists.data.SharedFilm>
    ) = write("Unable to import these films. Retry when connected.") {
        require(films.size in 1..MAX_IMPORT_BATCH_SIZE && films.all { it.id > 0 }) { "Choose up to 50 valid films per batch." }
        database.importMovies(requestId, listId, films.distinctBy { it.id }, clock.millis())
    }

    private fun validateName(name: String): String =
        name.trim().also { require(it.isNotBlank()) { "Enter a list name." } }

    private suspend fun <T> write(message: String, operation: suspend () -> T): Result<T> = recoverableResult {
        operation()
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { error ->
            Result.failure(
                if (preservesFailure(error)) error else IllegalStateException(message, error)
            )
        },
    )

    private fun preservesFailure(error: Throwable): Boolean = when (error) {
        is WatchlistNotFoundException, is IllegalArgumentException,
        is com.jcotters.contract.watchlists.data.SharedConnectionException,
        is com.jcotters.contract.watchlists.data.SharedAccessDeniedException,
        is com.jcotters.contract.watchlists.data.ImportUnavailableException,
        is com.jcotters.contract.watchlists.data.SharingUnavailableException -> true

        else -> false
    }
}

private fun WatchlistRecord.toDomain() = Watchlist(id, name, createdAt, movieCount, shared)

package com.jcotters.internal.watchlists

import com.jcotters.contract.time.AppClock
import com.jcotters.contract.watchlists.data.WatchlistDatabase
import com.jcotters.contract.watchlists.data.WatchlistRecord
import com.jcotters.contract.watchlists.domain.IWatchlistsRepository
import com.jcotters.contract.watchlists.domain.Watchlist
import com.jcotters.contract.watchlists.domain.WatchlistContent
import com.jcotters.contract.watchlists.domain.WatchlistNotFoundException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

internal class WatchlistsRepository(private val database: WatchlistDatabase, private val clock: AppClock) : IWatchlistsRepository {
    override fun observeWatchlists() = database.observeLists().map { rows -> rows.map(WatchlistRecord::toDomain) }
    override fun observeWatchlist(id: String) = database.observeContent(id).map { it?.let { content -> WatchlistContent(content.list.toDomain(), content.movies) } }
    override fun observeMovieIds(listId: String) = database.observeMovieIds(listId)
    override fun observeListIds(movieId: Int) = database.observeListIds(movieId)

    override suspend fun create(name: String): Result<String> = write("Unable to create the list.") {
        val validName = validateName(name)
        val id = Uuid.random().toString()
        database.create(id, validName, clock.millis())
        id
    }

    override suspend fun rename(id: String, name: String) = write("Unable to rename the list.") { database.rename(id, validateName(name)) }
    override suspend fun delete(id: String) = write("Unable to delete the list.") { database.delete(id) }
    override suspend fun addMovieToLists(movieId: Int, listIds: Set<String>) = write("Unable to add this movie to the selected lists.") {
        require(listIds.isNotEmpty()) { "Choose at least one list." }
        database.addMovieToLists(movieId, listIds, clock.millis())
    }
    override suspend fun removeMovie(listId: String, movieId: Int) = write("Unable to remove this movie from the list.") { database.removeMovie(listId, movieId) }

    private fun validateName(name: String): String = name.trim().also { require(it.isNotBlank()) { "Enter a list name." } }

    private suspend fun <T> write(message: String, operation: suspend () -> T): Result<T> = try {
        Result.success(operation())
    } catch (error: CancellationException) {
        throw error
    } catch (error: WatchlistNotFoundException) {
        Result.failure(error)
    } catch (error: IllegalArgumentException) {
        Result.failure(error)
    } catch (error: Exception) {
        Result.failure(IllegalStateException(message, error))
    }
}

private fun WatchlistRecord.toDomain() = Watchlist(id, name, createdAt, movieCount)

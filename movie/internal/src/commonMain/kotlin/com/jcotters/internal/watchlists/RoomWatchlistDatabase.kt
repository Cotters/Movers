package com.jcotters.internal.watchlists

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.jcotters.contract.watchlists.data.FilmImportOutcome
import com.jcotters.contract.watchlists.data.ImportBatchResult
import com.jcotters.contract.watchlists.data.ImportDisposition
import com.jcotters.contract.watchlists.data.WatchlistContentRecord
import com.jcotters.contract.watchlists.data.WatchlistDatabase
import com.jcotters.contract.watchlists.data.WatchlistRecord
import com.jcotters.contract.watchlists.domain.WatchlistNotFoundException
import com.jcotters.database.MoversDatabase
import com.jcotters.database.watchlists.DbWatchlist
import com.jcotters.database.watchlists.DbWatchlistMovie
import com.jcotters.database.watchlists.WatchlistEntryWithMovie
import com.jcotters.internal.detail.data.MovieMapper
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class RoomWatchlistDatabase(private val database: MoversDatabase, private val mapper: MovieMapper) :
    WatchlistDatabase {
    private val dao = database.watchlistDao()
    private val memberships = database.watchlistMembershipDao()

    override fun observeLists() = dao.observeLists().map { rows ->
        rows.map { WatchlistRecord(it.id, it.name, it.createdAt, it.movieCount) }
    }.distinctUntilChanged()

    override fun observeContent(id: String) = dao.observeContent(id).map { content ->
        content?.let {
            val movies = it.entries.sortedWith(
                compareByDescending<WatchlistEntryWithMovie> { entry -> entry.entry.addedAt }
                    .thenByDescending { entry -> entry.entry.movieId },
            ).map { entry -> mapper.toDomainModel(entry.movie) }
            WatchlistContentRecord(WatchlistRecord(it.list.id, it.list.name, it.list.createdAt, movies.size), movies)
        }
    }.distinctUntilChanged()

    override fun observeMovieIds(listId: String) =
        memberships.observeMovieIds(listId).map { it.toSet() }.distinctUntilChanged()

    override fun observeListIds(movieId: Int) =
        memberships.observeListIds(movieId).map { it.toSet() }.distinctUntilChanged()

    override suspend fun create(id: String, name: String, createdAt: Long) =
        dao.insert(DbWatchlist(id, name, createdAt))

    override suspend fun rename(id: String, name: String) {
        if (dao.rename(id, name) == 0) throw WatchlistNotFoundException()
    }

    override suspend fun delete(id: String) {
        if (dao.delete(id) == 0) throw WatchlistNotFoundException()
    }

    override suspend fun addMovieToLists(movieId: Int, listIds: Set<String>, addedAt: Long) {
        require(listIds.isNotEmpty()) { "Choose at least one list." }
        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                if (dao.countLists(listIds.toList()) != listIds.size) throw WatchlistNotFoundException()
                listIds.forEach { memberships.insertMovie(DbWatchlistMovie(it, movieId, addedAt)) }
            }
        }
    }

    override suspend fun removeMovie(listId: String, movieId: Int) = memberships.removeMovie(listId, movieId)

    override suspend fun importMovies(
        requestId: String,
        listId: String,
        films: List<com.jcotters.contract.watchlists.data.SharedFilm>,
        addedAt: Long
    ): ImportBatchResult =
        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                if (dao.get(listId) == null) throw WatchlistNotFoundException()
                ImportBatchResult(films.map { it.id }.distinct().map { id ->
                    val disposition = if (database.movieDao().getMovieById(id) == null) {
                        ImportDisposition.Rejected
                    } else if (memberships.insertMovie(DbWatchlistMovie(listId, id, addedAt)) == -1L) {
                        ImportDisposition.AlreadyPresent
                    } else ImportDisposition.Imported
                    FilmImportOutcome(id, disposition)
                })
            }
        }
}

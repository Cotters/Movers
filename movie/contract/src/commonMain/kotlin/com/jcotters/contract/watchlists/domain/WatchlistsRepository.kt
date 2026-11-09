package com.jcotters.contract.watchlists.domain

import com.jcotters.contract.detail.domain.models.Movie
import kotlinx.coroutines.flow.Flow

data class Watchlist(val id: String, val name: String, val createdAt: Long, val movieCount: Int)
data class WatchlistContent(val watchlist: Watchlist, val movies: List<Movie>)

class WatchlistNotFoundException : IllegalStateException("This list no longer exists.")

interface IWatchlistsRepository {
    fun observeWatchlists(): Flow<List<Watchlist>>
    fun observeWatchlist(id: String): Flow<WatchlistContent?>
    fun observeMovieIds(listId: String): Flow<Set<Int>>
    fun observeListIds(movieId: Int): Flow<Set<String>>
    suspend fun create(name: String): Result<String>
    suspend fun rename(id: String, name: String): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
    suspend fun addMovieToLists(movieId: Int, listIds: Set<String>): Result<Unit>
    suspend fun removeMovie(listId: String, movieId: Int): Result<Unit>
}

package com.jcotters.contract.watchlists.data

import com.jcotters.contract.detail.domain.models.Movie
import kotlinx.coroutines.flow.Flow

data class WatchlistRecord(val id: String, val name: String, val createdAt: Long, val movieCount: Int, val shared: SharedListInfo? = null)
data class WatchlistContentRecord(val list: WatchlistRecord, val movies: List<Movie>, val contributions: Map<Int, FilmContribution> = emptyMap())

/** Storage operations are provider-neutral; a multi-list addition must commit atomically. */
interface WatchlistDatabase {
    fun observeLists(): Flow<List<WatchlistRecord>>
    fun observeContent(id: String): Flow<WatchlistContentRecord?>
    fun observeMovieIds(listId: String): Flow<Set<Int>>
    fun observeListIds(movieId: Int): Flow<Set<String>>
    suspend fun create(id: String, name: String, createdAt: Long)
    suspend fun rename(id: String, name: String)
    suspend fun delete(id: String)
    suspend fun addMovieToLists(movieId: Int, listIds: Set<String>, addedAt: Long)
    suspend fun removeMovie(listId: String, movieId: Int)
    suspend fun importMovies(requestId: String, listId: String, films: List<SharedFilm>, addedAt: Long): ImportBatchResult
}

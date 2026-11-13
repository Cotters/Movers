package com.jcotters.contract.watchlists.domain

import com.jcotters.contract.detail.domain.models.Movie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class Watchlist(
    val id: String,
    val name: String,
    val createdAt: Long,
    val movieCount: Int,
    val shared: com.jcotters.contract.watchlists.data.SharedListInfo? = null
)

data class WatchlistContent(
    val watchlist: Watchlist,
    val movies: List<Movie>,
    val contributions: Map<Int, com.jcotters.contract.watchlists.data.FilmContribution> = emptyMap()
)

class WatchlistNotFoundException : IllegalStateException("This list no longer exists.")

data class MembershipAcknowledgement(val revision: Long?)

interface IWatchlistsRepository {
    fun observeWatchlists(): Flow<List<Watchlist>>
    fun observeWatchlist(id: String): Flow<WatchlistContent?>
    fun observeMovieIds(listId: String): Flow<Set<Int>>
    fun observeListIds(movieId: Int): Flow<Set<String>>
    suspend fun setMembership(listId: String, movieId: Int, present: Boolean): Result<MembershipAcknowledgement> {
        val result = if (present) addMovieToLists(movieId, setOf(listId)) else removeMovie(listId, movieId)
        return result.map { MembershipAcknowledgement(observeWatchlist(listId).first()?.watchlist?.shared?.revision) }
    }

    suspend fun create(name: String): Result<String>
    suspend fun rename(id: String, name: String): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
    suspend fun addMovieToLists(movieId: Int, listIds: Set<String>): Result<Unit>
    suspend fun removeMovie(listId: String, movieId: Int): Result<Unit>
    suspend fun importMovies(
        requestId: String,
        listId: String,
        films: List<com.jcotters.contract.watchlists.data.SharedFilm>
    ): Result<com.jcotters.contract.watchlists.data.ImportBatchResult>
}

package com.jcotters.internal.watchlists

import com.jcotters.contract.watchlists.data.SharedCommand
import com.jcotters.contract.watchlists.data.SharedFilm
import com.jcotters.contract.watchlists.data.SharedSnapshot
import com.jcotters.contract.watchlists.data.WatchlistDatabase
import com.jcotters.database.MoversDatabase
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.Json

internal class CollaborativeWatchlistDatabase(
    private val local: RoomWatchlistDatabase,
    private val database: MoversDatabase,
    private val sharing: SharedWatchlistsRepository,
) : WatchlistDatabase {
    private val json = Json { ignoreUnknownKeys = true }
    private val metadata = database.sharedCacheDao().observeLists()
    override fun observeLists() = combine(local.observeLists(), metadata) { lists, cache ->
        val snapshots = cache.associate { it.listId to json.decodeFromString<SharedSnapshot>(it.payload) }
        lists.map { it.copy(shared = snapshots[it.id]?.info) }
    }
    override fun observeContent(id: String) = combine(local.observeContent(id), metadata) { content, cache ->
        val snapshot = cache.firstOrNull { it.listId == id }?.let { json.decodeFromString<SharedSnapshot>(it.payload) }
        content?.copy(list = content.list.copy(shared = snapshot?.info), contributions = snapshot?.entries.orEmpty().associate { it.film.id to it.contribution })
    }
    override fun observeMovieIds(listId: String) = local.observeMovieIds(listId)
    override fun observeListIds(movieId: Int) = local.observeListIds(movieId)
    override suspend fun create(id: String, name: String, createdAt: Long) = local.create(id,name,createdAt)
    override suspend fun rename(id: String, name: String) = sharing.write(setOf(id), { local.rename(id,name) }, { SharedCommand.Rename(it,name) })
    override suspend fun delete(id: String) {
        sharing.write(setOf(id), { local.delete(id) }, { error("Shared lists can be left, not deleted.") })
    }
    override suspend fun addMovieToLists(movieId: Int, listIds: Set<String>, addedAt: Long) = sharing.write(listIds,
        { local.addMovieToLists(movieId,listIds,addedAt) }, { SharedCommand.SetFilm(it, film(movieId), true) })
    override suspend fun removeMovie(listId: String, movieId: Int) = sharing.write(setOf(listId),
        { local.removeMovie(listId,movieId) }, { SharedCommand.SetFilm(it, film(movieId), false) })
    override suspend fun importMovies(requestId: String, listId: String, films: List<SharedFilm>, addedAt: Long) =
        sharing.importMovies(requestId, listId, films) {
            local.importMovies(requestId, listId, films, addedAt)
        }
    private suspend fun film(id: Int): SharedFilm {
        val movie = database.movieDao().getMovieById(id) ?: error("Movie is unavailable. Search again.")
        return SharedFilm(id,movie.title,movie.synopsis,movie.releaseDate,movie.posterUrl)
    }
}

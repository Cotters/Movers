package com.jcotters.internal.watchlists

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.jcotters.contract.watchlists.data.ActivityPage
import com.jcotters.contract.watchlists.data.SharedFilm
import com.jcotters.contract.watchlists.data.SharedSnapshot
import com.jcotters.database.MoversDatabase
import com.jcotters.database.movies.DbMovie
import com.jcotters.database.watchlists.DbSharedActivity
import com.jcotters.database.watchlists.DbSharedActivityCursor
import com.jcotters.database.watchlists.DbSharedList
import com.jcotters.database.watchlists.DbWatchlist
import com.jcotters.database.watchlists.DbWatchlistMovie
import kotlinx.serialization.json.Json

/** Persists shared snapshots and activity without changing the operation owner's identity. */
internal class SharedListCache(private val database: MoversDatabase, private val json: Json) {
    private val cache = database.sharedCacheDao()
    private val lists = database.watchlistDao()
    private val memberships = database.watchlistMembershipDao()
    private val outbox = database.sharedOperationDao()
    private val activity = database.sharedActivityDao()

    suspend fun checkOwner(id: String) {
        check(cache.lists().all { it.participantId == id } && outbox.operations().all { it.participantId == id }) {
            "Saved lists belong to a different device identity. Sharing is unavailable."
        }
    }

    suspend fun saveSnapshot(id: String, snapshot: SharedSnapshot, owner: String) {
        database.useWriterConnection { connection ->
            connection.immediateTransaction<Unit> {
                if ((cache.get(id)?.revision ?: -1) <= snapshot.info.revision) {
                    database.movieDao().insertMissingMovies(snapshot.entries.map { it.film.toDb() })
                    lists.upsert(DbWatchlist(id, snapshot.name, snapshot.createdAt))
                    memberships.clearMovies(id)
                    snapshot.entries.forEach {
                        memberships.insertMovie(
                            DbWatchlistMovie(
                                id,
                                it.film.id,
                                it.contribution.addedAt
                            )
                        )
                    }
                    cache.put(DbSharedList(id, owner, snapshot.info.revision, json.encodeToString(snapshot)))
                }
            }
        }
    }

    suspend fun saveActivity(id: String, page: ActivityPage) {
        database.movieDao().insertMissingMovies(page.events.map { it.film.toDb() })
        database.useWriterConnection { connection ->
            connection.immediateTransaction<Unit> {
                activity.putActivity(page.events.map {
                    DbSharedActivity(
                        id,
                        it.id,
                        it.sequence,
                        json.encodeToString(it)
                    )
                })
                activity.putCursor(DbSharedActivityCursor(id, page.nextBefore))
            }
        }
    }

    suspend fun purge(id: String) {
        database.useWriterConnection { connection ->
            connection.immediateTransaction<Unit> {
                cache.remove(id)
                activity.clearActivity(id)
                activity.clearCursor(id)
                outbox.discard(id)
                lists.delete(id)
            }
        }
    }
}

private fun SharedFilm.toDb() = DbMovie(id, title, synopsis, releaseDate, posterUrl)

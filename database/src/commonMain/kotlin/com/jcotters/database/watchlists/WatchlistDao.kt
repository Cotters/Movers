package com.jcotters.database.watchlists

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query(
        """
        SELECT w.id, w.name, w.createdAt, COUNT(m.movieId) AS movieCount
        FROM watchlists w LEFT JOIN watchlist_movies m ON m.watchlistId = w.id
        GROUP BY w.id ORDER BY w.createdAt DESC, w.id DESC
    """
    )
    fun observeLists(): Flow<List<DbWatchlistSummary>>

    @Transaction
    @Query("SELECT * FROM watchlists WHERE id = :id")
    fun observeContent(id: String): Flow<WatchlistWithEntries?>

    @Insert
    suspend fun insert(list: DbWatchlist)

    @Query("UPDATE watchlists SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String): Int

    @Query("DELETE FROM watchlists WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("SELECT * FROM watchlists WHERE id = :id")
    suspend fun get(id: String): DbWatchlist?

    @androidx.room.Upsert
    suspend fun upsert(list: DbWatchlist)

    @Query("SELECT COUNT(*) FROM watchlists WHERE id IN (:ids)")
    suspend fun countLists(ids: List<String>): Int
}

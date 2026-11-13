package com.jcotters.database.watchlists

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistMembershipDao {
    @Query("SELECT movieId FROM watchlist_movies WHERE watchlistId = :listId")
    fun observeMovieIds(listId: String): Flow<List<Int>>

    @Query("SELECT watchlistId FROM watchlist_movies WHERE movieId = :movieId")
    fun observeListIds(movieId: Int): Flow<List<String>>

    @Query("DELETE FROM watchlist_movies WHERE watchlistId = :id")
    suspend fun clearMovies(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMovie(entry: DbWatchlistMovie): Long

    @Query("DELETE FROM watchlist_movies WHERE watchlistId = :listId AND movieId = :movieId")
    suspend fun removeMovie(listId: String, movieId: Int)
}

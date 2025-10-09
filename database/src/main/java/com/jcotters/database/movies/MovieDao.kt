package com.jcotters.database.movies

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getMovieById(id: Int): DbMovie?

    @Query("SELECT * FROM movies")
    suspend fun getAllMovies(): List<DbMovie>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: DbMovie)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<DbMovie>)

    @Delete
    suspend fun deleteMovie(movie: DbMovie)

    @Query("DELETE FROM movies")
    suspend fun clearAll()
}


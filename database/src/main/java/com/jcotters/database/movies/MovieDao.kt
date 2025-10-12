package com.jcotters.database.movies

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface MovieDao {
  @Query("SELECT * FROM movies WHERE id = :id")
  suspend fun getMovieById(id: Int): DbMovie?

  @Query("SELECT * FROM movies")
  suspend fun getAllMovies(): List<DbMovie>

  @Query("SELECT MAX(page) FROM movies")
  suspend fun getLastPage(): Int?

  @Query("SELECT * FROM movies ORDER BY page ASC")
  fun getMoviesPagingSource(): PagingSource<Int, DbMovie>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMovie(movie: DbMovie)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMovies(movies: List<DbMovie>)

  @Delete
  suspend fun deleteMovie(movie: DbMovie)

  @Query("DELETE FROM movies")
  suspend fun clearAll()

  @Transaction
  suspend fun refreshMovies(movies: List<DbMovie>) {
    clearAll()
    upsertAll(movies)
  }

  @Upsert
  suspend fun upsertAll(movies: List<DbMovie>)
}


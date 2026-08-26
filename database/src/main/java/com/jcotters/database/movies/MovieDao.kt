package com.jcotters.database.movies

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getMovieById(id: Int): DbMovie?

    @Query("SELECT * FROM movies")
    suspend fun getAllMovies(): List<DbMovie>

    @Query(
        """
        SELECT movie.* FROM movies AS movie
        INNER JOIN movie_catalogue_entries AS entry
            ON entry.movieId = movie.id
        WHERE entry.catalogue = :catalogue
        ORDER BY entry.page ASC, entry.pageIndex ASC
        """
    )
    fun getPopularMoviesPagingSource(catalogue: String): PagingSource<Int, DbMovie>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: DbMovie)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<DbMovie>)

    @Upsert
    suspend fun upsertAll(movies: List<DbMovie>)
}


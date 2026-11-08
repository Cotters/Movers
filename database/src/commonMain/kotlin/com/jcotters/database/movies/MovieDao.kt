package com.jcotters.database.movies

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getMovieById(id: Int): DbMovie?

    @Query("SELECT * FROM movies WHERE id = :id")
    fun observeMovieById(id: Int): Flow<DbMovie?>

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

    @Upsert
    suspend fun insertMovie(movie: DbMovie)

    @Upsert
    suspend fun insertMovies(movies: List<DbMovie>)

    @Upsert
    suspend fun upsertAll(movies: List<DbMovie>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissingMovies(movies: List<DbMovie>)

    @Query("""
        UPDATE movies SET title = :title, synopsis = :synopsis, releaseDate = :releaseDate,
            posterUrl = COALESCE(:posterUrl, posterUrl), backdropUrl = COALESCE(:backdropUrl, backdropUrl),
            originalLanguage = COALESCE(:originalLanguage, originalLanguage),
            voteAverage = COALESCE(:voteAverage, voteAverage), voteCount = COALESCE(:voteCount, voteCount)
        WHERE id = :id
    """)
    suspend fun updateCatalogueFields(
        id: Int, title: String, synopsis: String, releaseDate: String,
        posterUrl: String?, backdropUrl: String?, originalLanguage: String?,
        voteAverage: Double?, voteCount: Int?,
    )

    /** Catalogue responses are partial and must never reset enrichment or its freshness. */
    @Transaction
    suspend fun upsertCatalogueMovies(movies: List<DbMovie>) {
        insertMissingMovies(movies)
        movies.forEach { movie ->
            updateCatalogueFields(
                movie.id, movie.title, movie.synopsis, movie.releaseDate,
                movie.posterUrl, movie.backdropUrl, movie.originalLanguage,
                movie.voteAverage, movie.voteCount,
            )
        }
    }

    @Transaction
    suspend fun saveMovieDetails(movie: DbMovie) {
        val cached = getMovieById(movie.id)
        insertMovie(movie.copy(
            synopsis = movie.synopsis.ifBlank { cached?.synopsis.orEmpty() },
            releaseDate = movie.releaseDate.ifBlank { cached?.releaseDate.orEmpty() },
            posterUrl = movie.posterUrl ?: cached?.posterUrl,
            backdropUrl = movie.backdropUrl ?: cached?.backdropUrl,
            originalLanguage = movie.originalLanguage ?: cached?.originalLanguage,
            voteAverage = movie.voteAverage ?: cached?.voteAverage,
            voteCount = movie.voteCount ?: cached?.voteCount,
        ))
    }
}

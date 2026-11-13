package com.jcotters.internal.detail.data

import com.jcotters.contract.detail.domain.IMovieDetailsRepository
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.result.recoverableResult
import com.jcotters.contract.time.AppClock
import com.jcotters.database.movies.MovieDao
import com.jcotters.internal.MovieApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class MovieDetailsRepository(
    private val api: MovieApi,
    private val movieDao: MovieDao,
    private val movieMapper: MovieMapper,
    private val clock: AppClock,
) : IMovieDetailsRepository {
    // Same-movie callers reuse persisted results; different movies can refresh independently.
    private val refreshMutexes = mutableMapOf<Int, Mutex>()
    private val refreshMutexRegistry = Mutex()

    override fun observeMovieWithId(id: Int): Flow<Movie?> = movieDao.observeMovieById(id)
        .map { it?.let(movieMapper::toDomainModel) }
        .distinctUntilChanged()

    override suspend fun refreshMovieWithId(id: Int): Result<Unit> = recoverableResult {
        val movieMutex = refreshMutexRegistry.withLock { refreshMutexes.getOrPut(id) { Mutex() } }
        movieMutex.withLock {
            val fetchedAt = movieDao.getMovieById(id)?.detailsFetchedAt
            val now = clock.millis()
            if (fetchedAt != null && fetchedAt <= now && now - fetchedAt < CACHE_TTL_MILLIS) {
                return@withLock
            }
            val dto = api.getMovieById(movieId = id)
            require(dto.id == id) { "Movie details response did not match the requested movie." }
            val movie = movieMapper.toDatabaseModel(dto, fetchedAt = clock.millis())
                ?: throw NoSuchElementException("Movie details response omitted its title.")
            movieDao.saveMovieDetails(movie)
        }
    }.fold(
        onSuccess = { Result.success(Unit) },
        onFailure = { Result.failure(IllegalStateException("Unable to load additional movie details.", it)) },
    )

    private companion object {
        const val CACHE_TTL_MILLIS = 24 * 60 * 60 * 1000L
    }
}

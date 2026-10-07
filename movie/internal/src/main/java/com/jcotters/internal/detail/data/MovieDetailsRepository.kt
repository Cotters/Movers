package com.jcotters.internal.detail.data

import com.jcotters.contract.detail.domain.IMovieDetailsRepository
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.database.movies.MovieDao
import com.jcotters.internal.MovieApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MovieDetailsRepository @Inject constructor(
    private val api: MovieApi,
    private val movieDao: MovieDao,
    private val movieMapper: MovieMapper,
    @param:MovieDetailsClock private val clock: Clock,
) : IMovieDetailsRepository {
    // Same-movie callers reuse persisted results; different movies can refresh independently.
    private val refreshMutexes = ConcurrentHashMap<Int, Mutex>()

    override fun observeMovieWithId(id: Int): Flow<Movie?> = movieDao.observeMovieById(id)
        .map { it?.let(movieMapper::toDomainModel) }
        .distinctUntilChanged()

    override suspend fun refreshMovieWithId(id: Int): Result<Unit> = try {
        refreshMutexes.getOrPut(id) { Mutex() }.withLock {
            val fetchedAt = movieDao.getMovieById(id)?.detailsFetchedAt
            val now = clock.millis()
            if (fetchedAt != null && fetchedAt <= now && now - fetchedAt < CACHE_TTL_MILLIS) {
                return@withLock Result.success(Unit)
            }
            val dto = api.getMovieById(movieId = id)
            require(dto.id == id) { "Movie details response did not match the requested movie." }
            val movie = movieMapper.toDatabaseModel(dto, fetchedAt = clock.millis())
                ?: throw NoSuchElementException("Movie details response omitted its title.")
            movieDao.saveMovieDetails(movie)
            Result.success(Unit)
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(IllegalStateException("Unable to load additional movie details.", error))
    }

    private companion object {
        val CACHE_TTL_MILLIS = Duration.ofHours(24).toMillis()
    }
}

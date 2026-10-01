package com.jcotters.internal.catalogue.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.jcotters.database.MoversDatabase
import com.jcotters.database.movies.DbMovie
import com.jcotters.database.movies.MovieCatalogue
import com.jcotters.database.movies.DbMovieCatalogueRemoteKey
import com.jcotters.database.movies.MovieCatalogueDao
import com.jcotters.database.movies.MovieCatalogueRemoteKeysDao
import com.jcotters.database.movies.MovieDao
import com.jcotters.internal.MovieApi
import com.jcotters.internal.catalogue.data.models.CataloguePageResponse
import com.jcotters.internal.detail.data.MovieMapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalPagingApi::class)
class PopularMoviesRemoteMediatorShould {
    private val api = mockk<MovieApi>()
    private val movies = mockk<MovieDao>()
    private val catalogue = mockk<MovieCatalogueDao>()
    private val keys = mockk<MovieCatalogueRemoteKeysDao>()
    private val database = mockk<MoversDatabase> {
        every { movieDao() } returns movies
        every { movieCatalogueDao() } returns catalogue
        every { movieCatalogueRemoteKeysDao() } returns keys
    }
    private var now = Duration.ofDays(1).toMillis()
    private val clock = mockk<Clock> { every { millis() } answers { now } }
    private val mediator = PopularMoviesRemoteMediator(api, database, MovieMapper(), clock)
    private val pagingState = PagingState<Int, DbMovie>(emptyList(), null, PagingConfig(10), 0)

    @Test
    fun `skip refresh for a fresh cache then refresh when it expires`() = runBlocking {
        coEvery { keys.getRemoteKey(MovieCatalogue.Popular.key) } returns DbMovieCatalogueRemoteKey(MovieCatalogue.Popular.key, 2, now)
        assertEquals(RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH, mediator.initialize())
        now += Duration.ofHours(6).toMillis()
        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, mediator.initialize())
    }

    @Test
    fun `refresh caches migrated without a timestamp`() = runBlocking {
        coEvery { keys.getRemoteKey(MovieCatalogue.Popular.key) } returns DbMovieCatalogueRemoteKey(MovieCatalogue.Popular.key, 2)
        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, mediator.initialize())
    }

    @Test
    fun `refresh when no cache exists`() = runBlocking {
        coEvery { keys.getRemoteKey(MovieCatalogue.Popular.key) } returns null
        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, mediator.initialize())
    }

    @Test
    fun `refresh when the wall clock moves behind the stored timestamp`() = runBlocking {
        coEvery { keys.getRemoteKey(MovieCatalogue.Popular.key) } returns DbMovieCatalogueRemoteKey(MovieCatalogue.Popular.key, 2, now + 1)
        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, mediator.initialize())
    }

    @Test
    fun `leave saved content untouched when refresh fails`() = runBlocking {
        coEvery { api.getPopularMovies(1) } throws IOException("Offline")
        assertTrue(mediator.load(LoadType.REFRESH, pagingState) is RemoteMediator.MediatorResult.Error)
        coVerify(exactly = 0) { catalogue.removeAllForCatalogue(any()) }
        coVerify(exactly = 0) { movies.upsertAll(any()) }
        coVerify(exactly = 0) { keys.insertKey(any()) }
    }

    @Test
    fun `retain the cache when the server returns the wrong page`() = runBlocking {
        coEvery { api.getPopularMovies(1) } returns CataloguePageResponse(page = 2)
        assertTrue(mediator.load(LoadType.REFRESH, pagingState) is RemoteMediator.MediatorResult.Error)
        coVerify(exactly = 0) { catalogue.removeAllForCatalogue(any()) }
    }

    @Test
    fun `propagate cancelled refreshes`() {
        coEvery { api.getPopularMovies(1) } throws CancellationException()
        assertThrows(CancellationException::class.java) {
            runBlocking { mediator.load(LoadType.REFRESH, pagingState) }
        }
    }
}

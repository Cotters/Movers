package com.jcotters.internal.catalogue.data

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.Room
import androidx.room.useReaderConnection
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jcotters.contract.time.AppClock
import com.jcotters.database.MoversDatabase
import com.jcotters.database.movies.DbMovie
import com.jcotters.database.movies.MovieCatalogue
import com.jcotters.database.watchlists.DbWatchlist
import com.jcotters.database.watchlists.DbWatchlistMovie
import com.jcotters.internal.MovieApi
import com.jcotters.internal.catalogue.data.models.CatalogueMovieDto
import com.jcotters.internal.catalogue.data.models.CataloguePageResponse
import com.jcotters.internal.detail.data.MovieMapper
import com.jcotters.internal.detail.data.models.MovieDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.Duration

@OptIn(ExperimentalPagingApi::class)
@RunWith(AndroidJUnit4::class)
class PopularMoviesRemoteMediatorTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database = Room.inMemoryDatabaseBuilder<MoversDatabase>(context).setDriver(androidx.sqlite.driver.bundled.BundledSQLiteDriver()).build()
    private val api = FakeMovieApi()
    private val state = PagingState<Int, DbMovie>(emptyList(), null, PagingConfig(10), 0)
    private val startTime = Duration.ofDays(1).toMillis()

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun appendingDoesNotExtendFreshnessAndRefreshPreservesWatchlists() = runTest {
        assertTrue(mediator(startTime).load(LoadType.REFRESH, state) is RemoteMediator.MediatorResult.Success)
        database.watchlistDao().insert(DbWatchlist("saved", "Saved list", 1))
        database.watchlistDao().insertMovie(DbWatchlistMovie("saved", 1, 2))
        database.movieDao().saveMovieDetails(database.movieDao().getMovieById(1)!!.copy(
            tagline = "Keep this tagline", runtimeMinutes = 125, detailsFetchedAt = startTime,
        ))
        val later = startTime + Duration.ofHours(1).toMillis()
        assertTrue(mediator(later).load(LoadType.APPEND, state) is RemoteMediator.MediatorResult.Success)
        assertEquals(startTime, database.movieCatalogueRemoteKeysDao().getRemoteKey(MovieCatalogue.Popular.key)?.lastRefreshedAt)
        assertEquals(2, database.movieDao().getAllMovies().size)
        val expired = startTime + Duration.ofHours(6).toMillis()
        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, mediator(expired).initialize())
        api.title = "Updated movie"
        assertTrue(mediator(expired).load(LoadType.REFRESH, state) is RemoteMediator.MediatorResult.Success)
        assertEquals(expired, database.movieCatalogueRemoteKeysDao().getRemoteKey(MovieCatalogue.Popular.key)?.lastRefreshedAt)
        val savedMovie = checkNotNull(database.watchlistDao().observeContent("saved").first()).entries.single().movie
        assertEquals(1, savedMovie.id)
        assertEquals("Updated movie", savedMovie.title)
        assertEquals("Keep this tagline", savedMovie.tagline)
        assertEquals(125, savedMovie.runtimeMinutes)
        assertEquals(startTime, savedMovie.detailsFetchedAt)
    }

    @Test
    fun failedRefreshRetainsMoviesEntriesAndTimestamp() = runTest {
        mediator(startTime).load(LoadType.REFRESH, state)
        val before = database.movieDao().getAllMovies()
        val key = database.movieCatalogueRemoteKeysDao().getRemoteKey(MovieCatalogue.Popular.key)
        api.offline = true
        val result = mediator(startTime + Duration.ofHours(7).toMillis()).load(LoadType.REFRESH, state)
        assertTrue(result is RemoteMediator.MediatorResult.Error)
        assertEquals(before, database.movieDao().getAllMovies())
        assertEquals(key, database.movieCatalogueRemoteKeysDao().getRemoteKey(MovieCatalogue.Popular.key))
val entryCount = database.useReaderConnection { connection ->
    connection.usePrepared("SELECT COUNT(*) FROM movie_catalogue_entries") { statement ->
        statement.step()
        statement.getLong(0)
    }
}
assertEquals(1L, entryCount)
    }

    private fun mediator(time: Long) = PopularMoviesRemoteMediator(
        api, database, MovieMapper(), AppClock { time },
    )

    private class FakeMovieApi : MovieApi {
        var offline = false
        var title = "Saved movie"

        override suspend fun getPopularMovies(page: Int, sortBy: String): CataloguePageResponse {
            if (offline) throw IOException("Offline")
            return CataloguePageResponse(
                page = page,
                results = listOf(CatalogueMovieDto(id = page, title = title, overview = "Overview")),
                totalPages = 3,
            )
        }

        override suspend fun getMovieById(movieId: Int): MovieDto = error("Not used in catalogue tests")
        override suspend fun searchMovies(query: String, page: Int, year: Int?): CataloguePageResponse = error("Not used in catalogue tests")
        override suspend fun findMovie(imdbId: String): com.jcotters.internal.ExternalMovieResponse = error("Not used in catalogue tests")
    }
}

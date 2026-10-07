package com.jcotters.internal.catalogue.data

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jcotters.database.MoversDatabase
import com.jcotters.database.bookmarks.Bookmark
import com.jcotters.database.movies.DbMovie
import com.jcotters.database.movies.MovieCatalogue
import com.jcotters.database.user.User
import com.jcotters.internal.MovieApi
import com.jcotters.internal.catalogue.data.models.CatalogueMovieDto
import com.jcotters.internal.catalogue.data.models.CataloguePageResponse
import com.jcotters.internal.detail.data.MovieMapper
import com.jcotters.internal.detail.data.models.MovieDto
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalPagingApi::class)
@RunWith(AndroidJUnit4::class)
class PopularMoviesRemoteMediatorTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database = Room.inMemoryDatabaseBuilder(context, MoversDatabase::class.java).build()
    private val api = FakeMovieApi()
    private val state = PagingState<Int, DbMovie>(emptyList(), null, PagingConfig(10), 0)
    private val startTime = Duration.ofDays(1).toMillis()

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun appendingDoesNotExtendFreshnessAndRefreshPreservesBookmarks() = runTest {
        assertTrue(mediator(startTime).load(LoadType.REFRESH, state) is RemoteMediator.MediatorResult.Success)
        val userId = database.userDao().insertUser(User(username = "reader")).toInt()
        database.bookmarkDao().insertBookmark(Bookmark(1, userId))
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
        val bookmarkedMovie = database.bookmarkDao().getBookmarkedMoviesForUser(userId).single()
        assertEquals(1, bookmarkedMovie.id)
        assertEquals("Updated movie", bookmarkedMovie.title)
        assertEquals("Keep this tagline", bookmarkedMovie.tagline)
        assertEquals(125, bookmarkedMovie.runtimeMinutes)
        assertEquals(startTime, bookmarkedMovie.detailsFetchedAt)
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
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM movie_catalogue_entries").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
    }

    private fun mediator(time: Long) = PopularMoviesRemoteMediator(
        api, database, MovieMapper(), Clock.fixed(Instant.ofEpochMilli(time), ZoneOffset.UTC),
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
    }
}

package com.jcotters.database.movies

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jcotters.database.MoversDatabase
import com.jcotters.database.watchlists.DbWatchlist
import com.jcotters.database.watchlists.DbWatchlistMovie
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DbMovieTest {

    private lateinit var database: MoversDatabase
    private lateinit var movieDao: MovieDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder<MoversDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .build()
        movieDao = database.movieDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testWriteAndReadMovie() = runTest {
        val movieId = 1
        val movie = DbMovie(
            id = movieId,
            title = "Test Movie",
            synopsis = "A riveting test",
            releaseDate = "01/01/1999",
        )
        movieDao.insertMovie(movie)

        val movieById = movieDao.getMovieById(movieId)

        assertEquals(movie, movieById)
    }

    @Test
    fun deviceProfileKeepsItsOriginalInstallationId() = runTest {
        val dao = database.profileDao()
        dao.insert(com.jcotters.database.profile.DeviceProfile(installationId = "first", displayName = "Josh"))
        dao.insert(com.jcotters.database.profile.DeviceProfile(installationId = "second", displayName = "Josh"))
        assertEquals("first", dao.get()?.installationId)
    }

    @Test
    fun catalogueUpdatesPreserveEnrichmentAndRelationships() = runTest {
        val enriched = DbMovie(
            7, "Original", "Overview", "2026", posterUrl = "poster.jpg",
            tagline = "Saved tagline", runtimeMinutes = 125, genres = listOf(DbMovieGenre(18, "Drama")),
            voteAverage = 7.8, voteCount = 50, status = "Released", originalLanguage = "en", detailsFetchedAt = 123
        )
        movieDao.saveMovieDetails(enriched)
        database.watchlistDao().insert(DbWatchlist("saved", "Saved list", 1))
        database.watchlistMembershipDao().insertMovie(DbWatchlistMovie("saved", 7, 2))
        database.movieCatalogueDao().insertCatalogueEntries(listOf(DbMovieCatalogueEntry("popular", 7, 1, 0)))
        movieDao.upsertCatalogueMovies(
            listOf(
                DbMovie(
                    7,
                    "New title",
                    "New overview",
                    "2027",
                    voteAverage = 8.1,
                    voteCount = 100
                )
            )
        )
        val expected = enriched.copy(
            title = "New title",
            synopsis = "New overview",
            releaseDate = "2027",
            voteAverage = 8.1,
            voteCount = 100
        )
        assertEquals(expected, movieDao.getMovieById(7))
        assertEquals(expected, database.watchlistDao().observeContent("saved").first()?.entries?.single()?.movie)
        val page = movieDao.getPopularMoviesPagingSource("popular").load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false),
        )
        assertEquals(listOf(expected), (page as PagingSource.LoadResult.Page).data)
    }

    @Test
    fun sparseDetailsPreserveKnownBasicsAndCanClearUnavailableExtras() = runTest {
        movieDao.insertMovie(
            DbMovie(
                7, "Old title", "Known overview", "2026", "poster.jpg", "backdrop.jpg",
                tagline = "Old tagline", runtimeMinutes = 125, voteAverage = 7.8, voteCount = 50,
                originalLanguage = "en", detailsFetchedAt = 1
            )
        )
        movieDao.saveMovieDetails(DbMovie(7, "New title", "", "", genres = emptyList(), detailsFetchedAt = 2))
        val movie = movieDao.getMovieById(7)!!
        assertEquals("New title", movie.title)
        assertEquals("Known overview", movie.synopsis)
        assertEquals("2026", movie.releaseDate)
        assertEquals("poster.jpg", movie.posterUrl)
        assertEquals("backdrop.jpg", movie.backdropUrl)
        assertEquals("en", movie.originalLanguage)
        assertEquals(7.8, movie.voteAverage!!, 0.001)
        assertEquals(50, movie.voteCount)
        assertEquals(null, movie.tagline)
        assertEquals(null, movie.runtimeMinutes)
        assertEquals(emptyList<DbMovieGenre>(), movie.genres)
        assertEquals(2L, movie.detailsFetchedAt)
    }

}

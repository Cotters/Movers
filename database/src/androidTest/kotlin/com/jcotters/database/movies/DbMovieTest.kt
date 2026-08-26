package com.jcotters.database.movies

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jcotters.database.MoversDatabase
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
}

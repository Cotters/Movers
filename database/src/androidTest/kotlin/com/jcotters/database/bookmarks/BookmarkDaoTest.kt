package com.jcotters.database.bookmarks

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jcotters.database.MoversDatabase
import com.jcotters.database.movies.DbMovie
import com.jcotters.database.user.User
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookmarkDaoTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database = Room.inMemoryDatabaseBuilder(context, MoversDatabase::class.java).build()
    private val dao = database.bookmarkDao()

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun writesAreIdempotentAndObservable() = runTest {
        val userId = database.userDao().insertUser(User(username = "reader")).toInt()
        database.movieDao().upsertAll(listOf(DbMovie(7, "Saved movie", "Overview", "2026")))
        assertFalse(dao.observeMovieBookmark(userId, 7).first())
        val added = async(start = CoroutineStart.UNDISPATCHED) {
            dao.observeBookmarkedMovies(userId).first { it.isNotEmpty() }
        }
        dao.insertBookmark(Bookmark(7, userId))
        dao.insertBookmark(Bookmark(7, userId))
        assertEquals(listOf(7), added.await().map { it.id })
        assertTrue(dao.observeMovieBookmark(userId, 7).first())
        val removed = async(start = CoroutineStart.UNDISPATCHED) {
            dao.observeBookmarkedMovies(userId).first { it.isEmpty() }
        }
        dao.removeBookmark(userId, 7)
        dao.removeBookmark(userId, 7)
        assertEquals(emptyList<Int>(), removed.await().map { it.id })
        assertFalse(dao.observeMovieBookmark(userId, 7).first())
    }
}

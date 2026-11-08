package com.jcotters.internal.detail.data

import com.jcotters.contract.detail.domain.IBookmarksRepository
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.database.bookmarks.BookmarkDao
import com.jcotters.database.movies.DbMovie
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarksRepositoryShould {

    @RelaxedMockK
    private lateinit var bookmarkDao: BookmarkDao
    private val movieMapper = MovieMapper()

    private lateinit var underTest: IBookmarksRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        underTest = BookmarksRepository(bookmarkDao = bookmarkDao, movieMapper = movieMapper)
    }

    @Test
    fun `return failure when failed to add bookmark to database`() {
        coEvery { bookmarkDao.insertBookmark(any()) } throws IllegalStateException("Test error")

        val result = runBlocking { underTest.addBookmark(MOVIE_ID) }

        assertTrue(result.isFailure)
        assertThat(result.exceptionOrNull()!!.message, equalTo("Unable to bookmark this movie."))
    }

    @Test
    fun `add bookmark when successfully added to database`() {
        val result = runBlocking { underTest.addBookmark(MOVIE_ID) }

        assertTrue(result.isSuccess)
    }

    @Test
    fun `return failure when failed to remove bookmark from database`() {
        coEvery { bookmarkDao.removeBookmark(any()) } throws IllegalStateException("Test error")

        val result = runBlocking { underTest.removeBookmark(MOVIE_ID) }

        assertTrue(result.isFailure)
        assertThat(result.exceptionOrNull()!!.message, equalTo("Unable to remove bookmark."))
    }

    @Test
    fun `remove bookmark when successfully removed from database`() {
        val result = runBlocking { underTest.removeBookmark(MOVIE_ID) }

        assertTrue(result.isSuccess)
    }

    @Test
    fun `observe changes to saved movies without recreating the repository`() = runTest {
        val rows = MutableStateFlow(DB_MOVIES)
        every { bookmarkDao.observeBookmarkedMovies() } returns rows
        val observed = mutableListOf<List<Movie>>()
        backgroundScope.launch { underTest.observeBookmarks().collect { observed += it } }
        runCurrent()
        rows.value = emptyList()
        runCurrent()
        assertThat(observed.first(), equalTo(DB_MOVIES.map(movieMapper::toDomainModel)))
        assertTrue(observed.last().isEmpty())
    }

    @Test
    fun `observe bookmark status changes`() = runTest {
        val saved = MutableStateFlow(false)
        every { bookmarkDao.observeMovieBookmark(MOVIE_ID) } returns saved
        val observed = mutableListOf<Boolean>()
        backgroundScope.launch { underTest.observeMovieBookmark(MOVIE_ID).collect { observed += it } }
        runCurrent()
        saved.value = true
        runCurrent()
        assertThat(observed, equalTo(listOf(false, true)))
    }

    @Test
    fun `preserve cancellation when writing a bookmark`() {
        coEvery { bookmarkDao.insertBookmark(any()) } throws CancellationException()
        assertThrows(CancellationException::class.java) {
            runBlocking { underTest.addBookmark(MOVIE_ID) }
        }
    }

    private companion object {
        const val MOVIE_ID = 1
        const val USER_ID = 1
        val DB_MOVIES = listOf(
            DbMovie(id = 1, title = "Movie 1", synopsis = "Movie 1 synopsis", releaseDate = "2022"),
            DbMovie(id = 2, title = "Movie 2", synopsis = "Movie 2 synopsis", releaseDate = "2023"),
            DbMovie(id = 3, title = "Movie 3", synopsis = "Movie 3 synopsis", releaseDate = "2024"),
        )
    }
}
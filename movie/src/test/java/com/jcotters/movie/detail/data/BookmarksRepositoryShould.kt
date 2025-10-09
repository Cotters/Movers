package com.jcotters.movie.detail.data

import com.jcotters.database.bookmarks.BookmarkDao
import com.jcotters.database.movies.DbMovie
import com.jcotters.movie.detail.domain.IBookmarksRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
    coEvery { bookmarkDao.insertBookmark(any()) } throws Throwable("Test error")

    val result = runBlocking { underTest.addBookmark(MOVIE_ID, USER_ID) }

    assertTrue(result.isFailure)
    assertThat(result.exceptionOrNull()!!.message, equalTo("Unable to bookmark this movie."))
  }

  @Test
  fun `add bookmark when successfully added to database`() {
    val result = runBlocking { underTest.addBookmark(MOVIE_ID, USER_ID) }

    assertTrue(result.isSuccess)
  }

  @Test
  fun `return failure when failed to remove bookmark from database`() {
    coEvery { bookmarkDao.removeBookmark(any(), any()) } throws Throwable("Test error")

    val result = runBlocking { underTest.removeBookmark(MOVIE_ID, USER_ID) }

    assertTrue(result.isFailure)
    assertThat(result.exceptionOrNull()!!.message, equalTo("Unable to remove bookmark."))
  }

  @Test
  fun `remove bookmark when successfully removed from database`() {
    val result = runBlocking { underTest.removeBookmark(MOVIE_ID, USER_ID) }

    assertTrue(result.isSuccess)
  }

  @Test
  fun `return failure when failed to get bookmarks from database`() {
    coEvery { bookmarkDao.getBookmarkedMoviesForUser(USER_ID) } throws Throwable("Test error")

    val result = runBlocking { underTest.getUserBookmarks(USER_ID) }

    assertTrue(result.isFailure)
    assertThat(result.exceptionOrNull()!!.message, equalTo("Unable to get bookmarked movies."))
  }

  @Test
  fun `return list of movies when getting bookmarks`() {
    coEvery { bookmarkDao.getBookmarkedMoviesForUser(USER_ID) } returns DB_MOVIES

    val result = runBlocking { underTest.getUserBookmarks(USER_ID) }
    val movies = DB_MOVIES.map(movieMapper::toDomainModel)

    assertTrue(result.isSuccess)
    assertThat(result.getOrNull()!!, equalTo(movies))
  }

  @Test
  fun `use dao when checking if movie is bookmarked`() {
    coEvery { bookmarkDao.hasUserBookmarkedMovie(USER_ID, MOVIE_ID) } returns true
    var result = runBlocking { underTest.isMovieBookmarked(USER_ID, MOVIE_ID) }
    assertTrue(result.getOrNull()!!)

    coEvery { bookmarkDao.hasUserBookmarkedMovie(USER_ID, MOVIE_ID) } returns false
    result = runBlocking { underTest.isMovieBookmarked(USER_ID, MOVIE_ID) }
    assertFalse(result.getOrNull()!!)
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
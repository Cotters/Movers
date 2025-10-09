package com.jcotters.movie.detail.data

import com.jcotters.database.bookmarks.BookmarkDao
import com.jcotters.movie.detail.domain.IBookmarksRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BookmarksRepositoryShould {

  @RelaxedMockK
  private lateinit var bookmarkDao: BookmarkDao

  private lateinit var underTest: IBookmarksRepository

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    underTest = BookmarksRepository(bookmarkDao = bookmarkDao)
  }

  @Test
  fun `return failure when failed to save bookmark to database`() {
    coEvery { bookmarkDao.insertBookmark(any()) } throws Throwable("Test error")

    val result = runBlocking { underTest.bookmarkMovie(MOVIE_ID, USER_ID) }

    assertTrue(result.isFailure)
    assertThat(result.exceptionOrNull()!!.message, equalTo("Unable to bookmark this movie."))
  }

  @Test
  fun `bookmark film when successfully saved to database`() {
    val result = runBlocking { underTest.bookmarkMovie(MOVIE_ID, USER_ID) }

    assertTrue(result.isSuccess)
  }

  private companion object {
    const val MOVIE_ID = 1
    const val USER_ID = 1
  }
}
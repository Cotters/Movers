package com.jcotters.movers.domain

import com.jcotters.auth.domain.IUserRepository
import com.jcotters.contract.detail.domain.IBookmarksRepository
import com.jcotters.details.domain.BookmarkMovieUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class BookmarkMovieUseCaseShould {

    @RelaxedMockK
    private lateinit var userRepository: IUserRepository

    @RelaxedMockK
    private lateinit var bookmarksRepository: IBookmarksRepository

    private lateinit var underTest: BookmarkMovieUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        underTest = BookmarkMovieUseCase(
            userRepository = userRepository,
            bookmarksRepository = bookmarksRepository,
        )
    }

    @Test
    fun `return failure when no userId is found`() {
        coEvery { userRepository.getUserIdOrNull() } returns null

        val result = runBlocking { underTest.addBookmark(MOVIE_ID) }

        Assert.assertFalse(result.isSuccess)
        coVerify(exactly = 0) { bookmarksRepository.addBookmark(MOVIE_ID, any()) }
        MatcherAssert.assertThat(result.exceptionOrNull()!!.message, CoreMatchers.equalTo("You must be logged in."))
    }

    @Test
    fun `bookmark movie when userId found`() {
        coEvery { userRepository.getUserIdOrNull() } returns USER_ID

        val result = runBlocking { underTest.addBookmark(MOVIE_ID) }

        Assert.assertTrue(result.isSuccess)
        coVerify(exactly = 1) { bookmarksRepository.addBookmark(MOVIE_ID, USER_ID) }
    }

    @Test
    fun `return failure when removing bookmark given no userId is found`() {
        coEvery { userRepository.getUserIdOrNull() } returns null

        val result = runBlocking { underTest.addBookmark(MOVIE_ID) }

        Assert.assertFalse(result.isSuccess)
        coVerify(exactly = 0) { bookmarksRepository.removeBookmark(MOVIE_ID, any()) }
        MatcherAssert.assertThat(result.exceptionOrNull()!!.message, CoreMatchers.equalTo("You must be logged in."))
    }

    @Test
    fun `remove bookmark when userId found`() {
        coEvery { userRepository.getUserIdOrNull() } returns USER_ID

        val result = runBlocking { underTest.removeBookmark(MOVIE_ID) }

        Assert.assertTrue(result.isSuccess)
        coVerify(exactly = 1) { bookmarksRepository.removeBookmark(MOVIE_ID, USER_ID) }
    }

    private companion object {
        const val MOVIE_ID = 1
        const val USER_ID = 1
    }
}
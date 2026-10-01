package com.jcotters.profile.data

import com.jcotters.database.user.User
import com.jcotters.database.user.UserDao
import com.jcotters.profile.domain.Profile
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfileRepositoryShould {

  @RelaxedMockK
  private lateinit var userDao: UserDao

  private lateinit var repository: ProfileRepository

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    repository = ProfileRepository(userDao)
  }

  @Test
  fun `return failure when dao throws`() {
    val failure = RuntimeException("DB error")
    coEvery { userDao.getUserById(TEST_USER_ID) } throws failure

    val result = runBlocking { repository.fetchProfile(TEST_USER_ID) }

    assertTrue(result.isFailure)
    assertEquals(result.exceptionOrNull()!!.message, "Unable to find user profile.")
    assertSame(failure, result.exceptionOrNull()?.cause)
  }

  @Test
  fun `propagate cancellation while reading the profile`() {
    val cancellation = CancellationException("Screen closed")
    coEvery { userDao.getUserById(TEST_USER_ID) } throws cancellation

    val thrown = assertThrows(CancellationException::class.java) {
      runBlocking { repository.fetchProfile(TEST_USER_ID) }
    }

    assertSame(cancellation, thrown)
  }

  @Test
  fun `return a recoverable failure when the user no longer exists`() {
    coEvery { userDao.getUserById(TEST_USER_ID) } returns null

    val result = runBlocking { repository.fetchProfile(TEST_USER_ID) }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.cause is NoSuchElementException)
  }

  @Test
  fun `do not convert fatal errors into profile failures`() {
    coEvery { userDao.getUserById(TEST_USER_ID) } throws AssertionError("Broken invariant")

    assertThrows(AssertionError::class.java) {
      runBlocking { repository.fetchProfile(TEST_USER_ID) }
    }
  }

  @Test
  fun `fetch user and map to Profile successfully`() {
    coEvery { userDao.getUserById(TEST_USER_ID) } returns TEST_USER

    val result = runBlocking { repository.fetchProfile(TEST_USER_ID) }

    assertTrue(result.isSuccess)
    assertEquals(TEST_PROFILE, result.getOrNull())
    coVerify(exactly = 1) { userDao.getUserById(TEST_USER_ID) }
  }

  companion object {
    private const val TEST_USER_ID = 1
    private val TEST_USER = User(id = 1, username = "testuser")
    private val TEST_PROFILE = Profile(username = "testuser")
  }
}

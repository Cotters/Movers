package com.jcotters.profile.data

import com.jcotters.database.user.User
import com.jcotters.database.user.UserDao
import com.jcotters.profile.domain.Profile
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
    coEvery { userDao.getUserById(TEST_USER_ID) } throws RuntimeException("DB error")

    val result = runBlocking { repository.fetchProfile(TEST_USER_ID) }

    assertTrue(result.isFailure)
    assertEquals(result.exceptionOrNull()!!.message, "Unable to find user profile.")
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
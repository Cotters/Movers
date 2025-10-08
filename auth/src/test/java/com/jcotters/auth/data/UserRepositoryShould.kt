package com.jcotters.auth.data

import com.jcotters.auth.domain.IUserRepository
import com.jcotters.database.user.User
import com.jcotters.database.user.UserDao
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserRepositoryShould {

  @MockK
  private lateinit var passwordUtils: PasswordUtils

  @MockK
  private lateinit var secureStorage: SecureStorage

  @MockK
  private lateinit var userDao: UserDao

  private lateinit var underTest: IUserRepository

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    underTest = UserRepository(
      passwordUtils = passwordUtils,
      secureStorage = secureStorage,
      userDao = userDao,
    )
  }

  @Test
  fun `fail login auth when password is incorrect`() {
    coEvery { secureStorage.loadCredentials(USERNAME) } returns
        AuthStorageCredentials(salt = "salt", hash = "other_hashed_password")
    coEvery { passwordUtils.hashPassword(PASSWORD, any()) } returns HASHED_PASSWORD

    val result = runBlocking { underTest.login(USERNAME, PASSWORD) }

    assertTrue(result.isFailure)
    assertThat(result.exceptionOrNull()!!.message, equalTo("Incorrect username or password."))
  }

  @Test
  fun `fail login when user cannot be found`() {
    coEvery { userDao.findByUsername(USERNAME) } returns null

    val result = runBlocking { underTest.login(USERNAME, PASSWORD) }

    assertTrue(result.isFailure)
  }

  @Test
  fun `succeed login auth when password is correct`() {
    coEvery { secureStorage.loadCredentials(USERNAME) } returns
        AuthStorageCredentials(salt = "salt", hash = HASHED_PASSWORD)
    coEvery { passwordUtils.hashPassword(PASSWORD, any()) } returns HASHED_PASSWORD
    coEvery { userDao.findByUsername(USERNAME) } returns
        User(username = USERNAME, password = PASSWORD)

    val result = runBlocking { underTest.login(USERNAME, PASSWORD) }

    assertTrue(result.isSuccess)
  }

  private companion object {
    const val USERNAME = "username"
    const val PASSWORD = "password"
    const val HASHED_PASSWORD = "hashed_password"
  }
}
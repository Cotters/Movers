package com.jcotters.auth.data

import com.jcotters.auth.domain.IUserRepository
import com.jcotters.database.user.User
import com.jcotters.database.user.UserDao
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.SecureRandom

class UserRepositoryShould {

  @MockK
  private lateinit var passwordUtils: PasswordUtils

  @RelaxedMockK
  private lateinit var secureStorage: SecureStorage

  @RelaxedMockK
  private lateinit var sessionManager: SessionManager

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
      sessionManager = sessionManager,
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
        User(username = USERNAME)

    val result = runBlocking { underTest.login(USERNAME, PASSWORD) }

    assertTrue(result.isSuccess)
    assertThat(result.getOrNull()!!.username, equalTo(USERNAME))
  }

  @Test
  fun `fail sign up when username already taken`() {
    coEvery { userDao.findByUsername(USERNAME) } returns DB_USER

    runBlocking { underTest.signUp(USERNAME, PASSWORD) }

    coVerify(exactly = 0) { passwordUtils.generateSalt() }
    coVerify(exactly = 0) { passwordUtils.hashPassword(PASSWORD, any()) }
    coVerify(exactly = 0) { secureStorage.saveCredentials(USERNAME, any(), HASHED_PASSWORD) }
    coVerify(exactly = 0) { userDao.insertUser(any()) }
  }

  @Test
  fun `generate and store salt and hash when signing up`() {
    val salt = generateSalt()
    val saltHex = salt.toHexString()

    coEvery { userDao.findByUsername(USERNAME) } returns null
    coEvery { passwordUtils.generateSalt() } returns salt
    coEvery { passwordUtils.hashPassword(PASSWORD, salt) } returns HASHED_PASSWORD
    coEvery { secureStorage.saveCredentials(USERNAME, saltHex, HASHED_PASSWORD) } returns Unit

    runBlocking { underTest.signUp(USERNAME, PASSWORD) }

    coVerify(exactly = 1) { passwordUtils.generateSalt() }
    coVerify(exactly = 1) { passwordUtils.hashPassword(PASSWORD, salt) }
    coVerify(exactly = 1) { secureStorage.saveCredentials(USERNAME, saltHex, HASHED_PASSWORD) }
    coVerify(exactly = 1) { userDao.insertUser(any()) }
  }

  @Test
  fun `create user session`() {
    runBlocking{ underTest.createUserSession(USER_ID) }

    coVerify(exactly = 1) { sessionManager.createSession(userId = USER_ID) }
  }

  private fun generateSalt(): ByteArray {
    val salt = ByteArray(16)
    SecureRandom().nextBytes(salt)
    return salt
  }


  private companion object {
    const val USER_ID = 1
    const val USERNAME = "username"
    const val PASSWORD = "password"
    const val HASHED_PASSWORD = "hashed_password"
    val DB_USER = User(id = 1, username = USERNAME)
  }
}
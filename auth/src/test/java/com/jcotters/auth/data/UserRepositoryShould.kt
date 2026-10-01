package com.jcotters.auth.data

import android.database.sqlite.SQLiteConstraintException
import com.jcotters.auth.domain.IUserRepository
import com.jcotters.database.user.User
import com.jcotters.database.user.UserDao
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

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
    every { sessionManager.sessionState } returns MutableStateFlow(SessionState())
    coEvery { userDao.findByUsername(USERNAME) } returns User(id = USER_ID, username = USERNAME)
    coEvery { userDao.importCredentials(any(), any(), any()) } returns Unit
    every { passwordUtils.generateSalt() } returns byteArrayOf(15)
    every { passwordUtils.hashPassword(any(), any()) } returns HASHED_PASSWORD
    underTest = UserRepository(
      passwordUtils = passwordUtils,
      secureStorage = secureStorage,
      userDao = userDao,
      sessionManager = sessionManager,
      clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
    )
  }

  @Test
  fun `fail login auth when password is incorrect`() {
    coEvery { secureStorage.loadCredentials(USERNAME) } returns
        AuthStorageCredentials(salt = "0f", hash = "other_hashed_password")
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
        AuthStorageCredentials(salt = "0f", hash = HASHED_PASSWORD)
    coEvery { passwordUtils.hashPassword(PASSWORD, any()) } returns HASHED_PASSWORD
    coEvery { userDao.findByUsername(USERNAME) } returns
        User(username = USERNAME)

    val result = runBlocking { underTest.login(USERNAME, PASSWORD) }

    assertTrue(result.isSuccess)
    assertThat(result.getOrNull()!!.username, equalTo(USERNAME))
  }

  @Test
  fun `fail sign up when username already taken`() {
    coEvery { userDao.insertUser(any()) } throws SQLiteConstraintException("Username taken.")

    val result = runBlocking { underTest.signUp(USERNAME, PASSWORD) }

    assertTrue(result.isFailure)
    coVerify(exactly = 0) { secureStorage.saveCredentials(any(), any(), any()) }
  }

  @Test
  fun `store the account and credentials in one insert`() {
    coEvery { userDao.insertUser(any()) } returns USER_ID.toLong()

    val result = runBlocking { underTest.signUp(USERNAME, PASSWORD) }

    assertTrue(result.isSuccess)
    coVerify { userDao.insertUser(User(username = USERNAME, passwordSalt = "0f", passwordHash = HASHED_PASSWORD)) }
    coVerify(exactly = 0) { secureStorage.saveCredentials(any(), any(), any()) }
  }

  @Test
  fun `leave no account behind when hashing fails and allow retry`() {
    var savedUser: User? = null
    coEvery { userDao.insertUser(any()) } answers {
      check(savedUser == null)
      savedUser = firstArg()
      USER_ID.toLong()
    }
    every { passwordUtils.hashPassword(any(), any()) } throws IllegalStateException("Hashing failed")
    assertTrue(runBlocking { underTest.signUp(USERNAME, PASSWORD) }.isFailure)
    assertTrue(savedUser == null)
    every { passwordUtils.hashPassword(any(), any()) } returns HASHED_PASSWORD

    assertTrue(runBlocking { underTest.signUp(USERNAME, PASSWORD) }.isSuccess)
    assertThat(savedUser?.passwordHash, equalTo(HASHED_PASSWORD))
  }

  @Test
  fun `allow retry after an insert fails`() {
    coEvery { userDao.insertUser(any()) } throws java.io.IOException("Disk full")
    assertTrue(runBlocking { underTest.signUp(USERNAME, PASSWORD) }.isFailure)
    coEvery { userDao.insertUser(any()) } returns USER_ID.toLong()
    assertTrue(runBlocking { underTest.signUp(USERNAME, PASSWORD) }.isSuccess)
    coVerify(exactly = 0) { secureStorage.saveCredentials(any(), any(), any()) }
  }

  @Test
  fun `import legacy credentials without changing the user identity`() {
    coEvery { secureStorage.loadCredentials(USERNAME) } returns AuthStorageCredentials("0f", HASHED_PASSWORD)
    val result = runBlocking { underTest.login(USERNAME, PASSWORD) }
    assertThat(result.getOrThrow().userId, equalTo(USER_ID))
    coVerify { userDao.importCredentials(USER_ID, "0f", HASHED_PASSWORD) }
  }

  @Test
  fun `use Room credentials without reading legacy storage`() {
    coEvery { userDao.findByUsername(USERNAME) } returns
      User(USER_ID, USERNAME, "0f", HASHED_PASSWORD)
    assertTrue(runBlocking { underTest.login(USERNAME, PASSWORD) }.isSuccess)
    coVerify(exactly = 0) { secureStorage.loadCredentials(any()) }
  }

  @Test
  fun `retain legacy credentials when their import fails`() {
    coEvery { secureStorage.loadCredentials(USERNAME) } returns AuthStorageCredentials("0f", HASHED_PASSWORD)
    coEvery { userDao.importCredentials(any(), any(), any()) } throws java.io.IOException("Disk full")
    assertTrue(runBlocking { underTest.login(USERNAME, PASSWORD) }.isFailure)
    coEvery { userDao.importCredentials(any(), any(), any()) } returns Unit
    assertTrue(runBlocking { underTest.login(USERNAME, PASSWORD) }.isSuccess)
  }

  @Test
  fun `create user session`() {
    runBlocking{ underTest.createUserSession(USER_ID) }

    coVerify(exactly = 1) { sessionManager.createSession(userId = USER_ID) }
  }

  @Test
  fun `preserve the cause of a failed session write`() {
    val failure = java.io.IOException("Disk full")
    coEvery { sessionManager.createSession(USER_ID) } throws failure

    val result = runBlocking { underTest.createUserSession(USER_ID) }

    assertThat(result.exceptionOrNull()?.message, equalTo("Unable to create user session."))
    assertSame(failure, result.exceptionOrNull()?.cause)
  }

  @Test
  fun `propagate cancellation while reading login credentials`() {
    coEvery { secureStorage.loadCredentials(USERNAME) } throws CancellationException("Screen closed")
    assertThrows(CancellationException::class.java) {
      runBlocking { underTest.login(USERNAME, PASSWORD) }
    }
  }

  @Test
  fun `propagate cancellation during account creation`() {
    coEvery { userDao.insertUser(any()) } throws CancellationException("Screen closed")
    assertThrows(CancellationException::class.java) {
      runBlocking { underTest.signUp(USERNAME, PASSWORD) }
    }
  }

  @Test
  fun `propagate cancellation while creating the persisted session`() {
    coEvery { sessionManager.createSession(USER_ID) } throws CancellationException("Screen closed")
    assertThrows(CancellationException::class.java) {
      runBlocking { underTest.createUserSession(USER_ID) }
    }
  }

  private companion object {
    const val USER_ID = 1
    const val USERNAME = "username"
    const val PASSWORD = "password"
    const val HASHED_PASSWORD = "hashed_password"
  }
}

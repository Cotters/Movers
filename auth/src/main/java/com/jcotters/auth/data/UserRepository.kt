package com.jcotters.auth.data

import com.jcotters.auth.domain.IUserRepository
import com.jcotters.auth.domain.MoverUser
import com.jcotters.auth.domain.UserSession
import com.jcotters.database.user.User
import com.jcotters.database.user.UserDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserRepository @Inject constructor(
  private val passwordUtils: PasswordUtils,
  private val secureStorage: SecureStorage,
  private val userDao: UserDao,
  private val sessionManager: SessionManager,
) : IUserRepository {

  private companion object {
    const val USERNAME_TAKEN_MESSAGE = "Username already in use."
    const val INCORRECT_DETAILS = "Incorrect username or password."
    const val SESSION_FAILED_MESSAGE = "Unable to create user session."
  }

  override suspend fun getUserIdOrNull(): Int? {
    return sessionManager.sessionState.first().userId
  }

  override val userSession: Flow<UserSession> = sessionManager.sessionState.map { sessionState ->
    if (sessionState.expiresAt <= System.currentTimeMillis() || sessionState.userId == null) {
      return@map UserSession.NotAuthenticated
    } else {
      return@map UserSession.Authenticated(sessionState.userId)
    }
  }

  override suspend fun login(username: String, password: String): Result<MoverUser> = withContext(Dispatchers.IO) {
    try {
      val credentials = secureStorage.loadCredentials(username)
      val storedHashedPassword = credentials.hash ?: throw Throwable(INCORRECT_DETAILS)
      val salt = credentials.salt ?: throw Throwable(INCORRECT_DETAILS)
      val hashedPassword = passwordUtils.hashPassword(password = password, salt = salt.toBytes())
      if (storedHashedPassword == hashedPassword) {
        val user = userDao.findByUsername(username) ?: throw Throwable(INCORRECT_DETAILS)
        return@withContext Result.success(MoverUser(userId = user.id, username = user.username))
      } else {
        throw Throwable(INCORRECT_DETAILS)
      }
    } catch (e: Throwable) {
      return@withContext Result.failure(e)
    }
  }

  override suspend fun signUp(username: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      if (userDao.findByUsername(username) != null) {
        throw Throwable(USERNAME_TAKEN_MESSAGE)
      }
      val salt = passwordUtils.generateSalt()
      val hashedPassword = passwordUtils.hashPassword(password = password, salt = salt)
      secureStorage.saveCredentials(username = username, saltHex = salt.toHexString(), hashHex = hashedPassword)
      userDao.insertUser(User(username = username))
      return@withContext Result.success(Unit)
    } catch (e: Throwable) {
      return@withContext Result.failure(e)
    }
  }

  override suspend fun createUserSession(userId: Int): Result<Unit> = withContext(Dispatchers.IO) {
    return@withContext try {
      sessionManager.createSession(userId)
      Result.success(Unit)
    } catch (_: Exception) {
      Result.failure(Throwable(SESSION_FAILED_MESSAGE))
    }
  }

  override suspend fun logout() = withContext(Dispatchers.IO) {
    sessionManager.clearSession()
  }
}

fun String.toBytes(): ByteArray {
  val len = this.length
  val data = ByteArray(len / 2)
  var i = 0
  while (i < len) {
    data[i / 2] = ((Character.digit(this[i], 16) shl 4) + Character.digit(this[i + 1], 16)).toByte()
    i += 2
  }
  return data
}
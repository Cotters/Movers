package com.jcotters.auth.data

import com.jcotters.auth.domain.IUserRepository
import com.jcotters.database.user.User
import com.jcotters.database.user.UserDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserRepository @Inject constructor(
  private val passwordUtils: PasswordUtils,
  private val secureStorage: SecureStorage,
  private val userDao: UserDao,
) : IUserRepository {

  private companion object {
    const val USERNAME_TAKEN_MESSAGE = "Username already in use."
    const val INCORRECT_DETAILS = "Incorrect username or password."
  }

  override suspend fun login(username: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val credentials = secureStorage.loadCredentials(username)
      val storedHashedPassword = credentials.hash ?: throw Throwable(INCORRECT_DETAILS)
      val salt = credentials.salt ?: throw Throwable(INCORRECT_DETAILS)
      val hashedPassword = passwordUtils.hashPassword(password = password, salt = salt.toBytes())
      if (storedHashedPassword == hashedPassword) {
        userDao.findByUsername(username) ?: throw Throwable(INCORRECT_DETAILS)
        return@withContext Result.success(Unit)
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
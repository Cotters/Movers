package com.jcotters.auth.data

import android.database.sqlite.SQLiteConstraintException
import com.jcotters.auth.domain.IUserRepository
import com.jcotters.auth.domain.MoverUser
import com.jcotters.auth.domain.UserSession
import com.jcotters.database.user.User
import com.jcotters.database.user.UserDao
import java.io.IOException
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class UserRepository @Inject constructor(
    private val passwordUtils: PasswordUtils,
    private val secureStorage: SecureStorage,
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val clock: Clock,
) : IUserRepository {

    private val mutex = Mutex()
    private val sessionRechecks = MutableStateFlow(0L)

    private companion object {
        const val USERNAME_TAKEN_MESSAGE = "Username already in use."
        const val INCORRECT_DETAILS = "Incorrect username or password."
        const val SESSION_FAILED_MESSAGE = "Unable to create user session."
    }

    override suspend fun getUserIdOrNull(): Int? {
        return when (val session = userSession.first()) {
            is UserSession.Authenticated -> session.userId
            else -> null
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val userSession: Flow<UserSession> = sessionRechecks.flatMapLatest {
        sessionManager.sessionState.observeUserSession(clock).catch { error ->
            if (error is IOException) {
                emit(UserSession.NotAuthenticated)
            } else {
                throw error
            }
        }
    }.distinctUntilChanged()

    override fun refreshSession() {
        sessionRechecks.update { it + 1 }
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun signUp(username: String, password: String): Result<MoverUser> = withContext(Dispatchers.IO) {
        try {
            mutex.withLock {
                val newUser = User(username = username)
                val userId = userDao.insertUser(newUser).toInt()
                val salt = passwordUtils.generateSalt()
                val hashedPassword = passwordUtils.hashPassword(password = password, salt = salt)
                secureStorage.saveCredentials(
                    username = username,
                    saltHex = salt.toHexString(),
                    hashHex = hashedPassword
                )
                return@withContext Result.success(MoverUser(userId = userId, username = newUser.username))
            }
        } catch (e: SQLiteConstraintException) {
            return@withContext Result.failure(Throwable(USERNAME_TAKEN_MESSAGE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun createUserSession(userId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            sessionManager.createSession(userId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.failure(Throwable(SESSION_FAILED_MESSAGE))
        }
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        sessionManager.clearSession()
    }

    override suspend fun getUserCount(): Int = withContext(Dispatchers.IO) {
        return@withContext userDao.getUserCount()
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

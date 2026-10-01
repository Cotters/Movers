package com.jcotters.auth.data

import android.database.sqlite.SQLiteConstraintException
import com.jcotters.auth.domain.IUserRepository
import com.jcotters.auth.domain.MoverUser
import com.jcotters.auth.domain.UserSession
import com.jcotters.database.user.User
import com.jcotters.database.user.UserDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Clock
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val passwordUtils: PasswordUtils,
    private val secureStorage: SecureStorage,
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val clock: Clock,
    @param:AuthDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : IUserRepository {

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

    override suspend fun login(username: String, password: String): Result<MoverUser> = withContext(dispatcher) {
        try {
            val user = userDao.findByUsername(username) ?: throw IllegalArgumentException(INCORRECT_DETAILS)
            val credentials = if (user.passwordSalt != null && user.passwordHash != null) {
                AuthStorageCredentials(salt = user.passwordSalt, hash = user.passwordHash)
            } else {
                secureStorage.loadCredentials(username)
            }
            val salt = credentials.salt ?: throw IllegalArgumentException(INCORRECT_DETAILS)
            val storedHash = credentials.hash ?: throw IllegalArgumentException(INCORRECT_DETAILS)
            val hash = passwordUtils.hashPassword(password, salt.toBytes())
            require(storedHash == hash) { INCORRECT_DETAILS }
            if (user.passwordSalt == null || user.passwordHash == null) {
                userDao.importCredentials(user.id, salt, storedHash)
            }
            Result.success(MoverUser(userId = user.id, username = user.username))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun signUp(username: String, password: String): Result<MoverUser> = withContext(dispatcher) {
        try {
            val salt = passwordUtils.generateSalt()
            val hash = passwordUtils.hashPassword(password, salt)
            val user = User(username = username, passwordSalt = salt.toHexString(), passwordHash = hash)
            val userId = userDao.insertUser(user).toInt()
            Result.success(MoverUser(userId = userId, username = username))
        } catch (e: SQLiteConstraintException) {
            return@withContext Result.failure(IllegalArgumentException(USERNAME_TAKEN_MESSAGE, e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun createUserSession(userId: Int): Result<Unit> = withContext(dispatcher) {
        return@withContext try {
            sessionManager.createSession(userId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (error: Exception) {
            Result.failure(IllegalStateException(SESSION_FAILED_MESSAGE, error))
        }
    }

    override suspend fun logout() = withContext(dispatcher) {
        sessionManager.clearSession()
    }

    override suspend fun getUserCount(): Int = withContext(dispatcher) {
        return@withContext userDao.getUserCount()
    }
}

fun String.toBytes(): ByteArray {
    require(length % 2 == 0 && all { it.digitToIntOrNull(16) != null }) { "Invalid password salt" }
    return ByteArray(length / 2) { index ->
        substring(index * 2, index * 2 + 2).toInt(16).toByte()
    }
}

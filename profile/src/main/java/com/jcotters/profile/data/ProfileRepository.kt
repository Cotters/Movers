package com.jcotters.profile.data

import com.jcotters.database.user.UserDao
import com.jcotters.profile.domain.IProfileRepository
import com.jcotters.profile.domain.Profile
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class ProfileRepository @Inject constructor(
  private val userDao: UserDao,
) : IProfileRepository {

  companion object {
    const val NO_USER_PROFILE_MESSAGE = "Unable to find user profile."
  }

  override suspend fun fetchProfile(userId: Int): Result<Profile> {
    return try {
        val user = userDao.getUserById(userId)
            ?: throw NoSuchElementException(NO_USER_PROFILE_MESSAGE)
        Result.success(Profile(username = user.username))
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      Result.failure(IllegalStateException(NO_USER_PROFILE_MESSAGE, error))
    }
  }
}

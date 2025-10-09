package com.jcotters.profile.data

import com.jcotters.database.user.UserDao
import com.jcotters.profile.domain.IProfileRepository
import com.jcotters.profile.domain.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProfileRepository @Inject constructor(
  private val userDao: UserDao,
) : IProfileRepository {

  companion object {
    const val NO_USER_PROFILE_MESSAGE = "Unable to find user profile."
  }

  override suspend fun fetchProfile(userId: Int): Result<Profile> = withContext(Dispatchers.IO) {
    try {
      val user = userDao.getUserById(userId)
      if (user == null) throw NoSuchElementException(NO_USER_PROFILE_MESSAGE)
      return@withContext Result.success(Profile(username = user.username))
    } catch (_: Throwable) {
      return@withContext Result.failure(Throwable(NO_USER_PROFILE_MESSAGE))
    }
  }
}
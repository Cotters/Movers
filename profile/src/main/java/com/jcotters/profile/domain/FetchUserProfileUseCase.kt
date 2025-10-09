package com.jcotters.profile.domain

import javax.inject.Inject

class FetchUserProfileUseCase @Inject constructor(
  private val userRepository: IProfileRepository,
) {

  suspend fun invoke(userId: Int): Result<Profile> {
    return userRepository.fetchProfile(userId)
  }

}
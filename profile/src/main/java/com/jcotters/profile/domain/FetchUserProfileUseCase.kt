package com.jcotters.profile.domain

import com.jcotters.auth.domain.IUserRepository
import javax.inject.Inject

class FetchUserProfileUseCase @Inject constructor(
    private val userRepository: IUserRepository,
    private val profileRepository: IProfileRepository,
) {

    suspend fun invoke(): Result<Profile> {
        val userId = userRepository.getUserIdOrNull()
            ?: return Result.failure(Throwable("No user session found."))
        return profileRepository.fetchProfile(userId)
    }

}
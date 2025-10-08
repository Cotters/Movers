package com.jcotters.profile.domain

import com.jcotters.auth.domain.IUserRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val userRepository: IUserRepository,
) {
  suspend fun invoke() {
    userRepository.logout()
  }
}
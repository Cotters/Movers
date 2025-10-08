package com.jcotters.auth.domain

import javax.inject.Inject

class SignUpUseCase @Inject constructor(
  private val userRepository: IUserRepository,
) {

  suspend fun invoke(username: String, password: String): Result<Unit> {
    return userRepository.signUp(username, password)
  }

}
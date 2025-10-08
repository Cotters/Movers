package com.jcotters.auth.domain

import javax.inject.Inject

class LoginUseCase @Inject constructor(
  private val repository: IUserRepository,
) {

  private companion object {
    private const val INCORRECT_DETAILS_MESSAGE = "Incorrect login details."
  }

  suspend fun invoke(username: String, password: String): Result<Unit> {
    return repository.login(username, password)
  }

}

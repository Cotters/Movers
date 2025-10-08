package com.jcotters.auth.domain

import javax.inject.Inject

class LoginUseCase @Inject constructor() {

  private companion object {
    private const val USERNAME = "username"
    private const val PASSWORD = "password"
    private const val INCORRECT_DETAILS_MESSAGE = "Incorrect login details."
  }

  fun invoke(username: String, password: String): Result<Unit> {
    val detailsAreCorrect = username.equals(USERNAME, ignoreCase = false) &&
        password.equals(PASSWORD, ignoreCase = false)
    return if (detailsAreCorrect) Result.success(Unit)
    else Result.failure(Throwable(INCORRECT_DETAILS_MESSAGE))
  }

}

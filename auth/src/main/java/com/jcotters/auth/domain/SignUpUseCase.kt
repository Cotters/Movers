package com.jcotters.auth.domain

import javax.inject.Inject

class SignUpUseCase @Inject constructor(
  private val repository: IUserRepository,
) {

  suspend fun invoke(username: String, password: String): Result<Unit> {
    val signUpResult = repository.signUp(username, password)
    signUpResult
      .onSuccess { moverUser ->
        return repository.createUserSession(moverUser.userId)
      }
      .onFailure {
        return Result.failure(it)
      }
    return Result.failure(Throwable("Something went wrong."))

  }

}
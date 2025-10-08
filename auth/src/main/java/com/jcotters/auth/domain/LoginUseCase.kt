package com.jcotters.auth.domain

import javax.inject.Inject

class LoginUseCase @Inject constructor(
  private val repository: IUserRepository,
) {

  suspend fun invoke(username: String, password: String): Result<Unit> {
    val loginResult = repository.login(username, password)
    loginResult
      .onSuccess { moverUser ->
        return repository.createUserSession(moverUser.userId)
      }
      .onFailure {
        return Result.failure(it)
      }
    return Result.failure(Throwable("Something went wrong."))
  }

}

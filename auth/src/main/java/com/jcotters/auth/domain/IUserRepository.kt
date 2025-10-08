package com.jcotters.auth.domain

interface IUserRepository {
  suspend fun login(username: String, password: String): Result<Unit>
  suspend fun signUp(username: String, password: String): Result<Unit>
}

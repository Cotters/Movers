package com.jcotters.auth.domain

interface IUserRepository {
  suspend fun login(username: String, password: String): Result<MoverUser>
  suspend fun signUp(username: String, password: String): Result<Unit>
  suspend fun createUserSession(userId: Int): Result<Unit>
}

package com.jcotters.auth.domain

interface IUserRepository {
  suspend fun login(username: String, password: String): Result<Unit>
}

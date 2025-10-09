package com.jcotters.auth.domain

import kotlinx.coroutines.flow.Flow

interface IUserRepository {
  val userSession: Flow<UserSession>
  suspend fun getUserIdOrNull(): Int?
  suspend fun login(username: String, password: String): Result<MoverUser>
  suspend fun signUp(username: String, password: String): Result<Unit>
  suspend fun createUserSession(userId: Int): Result<Unit>
  suspend fun logout()
}

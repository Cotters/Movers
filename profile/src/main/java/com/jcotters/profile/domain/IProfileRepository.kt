package com.jcotters.profile.domain

interface IProfileRepository {
  suspend fun fetchProfile(userId: Int): Result<Profile>
}
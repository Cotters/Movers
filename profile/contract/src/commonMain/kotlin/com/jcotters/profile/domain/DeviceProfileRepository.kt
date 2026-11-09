package com.jcotters.profile.domain

import kotlinx.coroutines.flow.Flow

data class Profile(val installationId: String, val displayName: String, val color: Int = 0, val nameConfirmed: Boolean = true)

interface DeviceProfileRepository {
    val profile: Flow<Profile?>
    suspend fun initialize()
    suspend fun saveName(name: String)
}

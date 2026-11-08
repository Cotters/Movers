package com.jcotters.profile.domain

import kotlinx.coroutines.flow.Flow

data class Profile(val installationId: String, val displayName: String)

interface DeviceProfileRepository {
    val profile: Flow<Profile?>
    suspend fun initialize()
}

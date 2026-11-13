package com.jcotters.contract.watchlists.data

/** Backend-neutral registration, authenticated as the existing participant. */
enum class PushEnvironment(val wireName: String) { Sandbox("sandbox"), Production("production") }

data class PushRegistration(
    val deviceId: String,
    val environment: PushEnvironment,
    val token: String?,
    val enabled: Boolean,
)

interface PushRegistrations {
    val configured: Boolean
    suspend fun registerPush(registration: PushRegistration)
}

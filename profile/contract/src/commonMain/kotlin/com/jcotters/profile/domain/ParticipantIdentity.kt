package com.jcotters.profile.domain

/** Backend credentials and provider sessions never cross this boundary. */
interface ParticipantIdentity {
    val configured: Boolean
    suspend fun currentId(): String?
    suspend fun ensure(name: String, color: Int): String
    suspend fun update(name: String, color: Int)
}

package com.jcotters.contract.watchlists.data

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class Participant(val id: String, val name: String, val color: Int)
@Serializable
data class SharedFilm(val id: Int, val title: String, val synopsis: String, val releaseDate: String, val posterUrl: String? = null)
@Serializable
data class FilmContribution(val movieId: Int, val actor: Participant, val addedAt: Long, val imported: Boolean = false)
@Serializable
data class SharedEntry(val film: SharedFilm, val contribution: FilmContribution)
@Serializable
data class FilmActivity(val id: String, val sequence: Long, val actor: Participant, val film: SharedFilm, val added: Boolean, val at: Long, val imported: Boolean = false)
@Serializable
data class SharedListInfo(val creator: Participant, val members: List<Participant>, val updatedAt: Long, val revision: Long)
@Serializable
data class SharedSnapshot(val id: String, val name: String, val createdAt: Long, val info: SharedListInfo, val entries: List<SharedEntry>)
@Serializable
data class ActivityPage(val events: List<FilmActivity>, val nextBefore: Long? = null)
@Serializable
data class InvitePreview(val listId: String, val name: String, val creator: Participant, val memberCount: Int, val movieCount: Int)

/** Mutations are online commands. Repeating a request ID must return its original outcome. */
@Serializable
sealed interface SharedCommand {
    val listId: String
    @Serializable @kotlinx.serialization.SerialName("Publish") data class Publish(override val listId: String, val name: String, val films: List<SharedFilm>) : SharedCommand
    @Serializable @kotlinx.serialization.SerialName("SetFilm") data class SetFilm(override val listId: String, val film: SharedFilm, val present: Boolean) : SharedCommand
    @Serializable @kotlinx.serialization.SerialName("ImportFilms") data class ImportFilms(override val listId: String, val films: List<SharedFilm>) : SharedCommand
    @Serializable @kotlinx.serialization.SerialName("Rename") data class Rename(override val listId: String, val name: String) : SharedCommand
    @Serializable @kotlinx.serialization.SerialName("Join") data class Join(override val listId: String, val token: String) : SharedCommand
    @Serializable @kotlinx.serialization.SerialName("Leave") data class Leave(override val listId: String) : SharedCommand
    @Serializable @kotlinx.serialization.SerialName("RevokeInvites") data class RevokeInvites(override val listId: String) : SharedCommand
}

interface SharedWatchlistRemote {
    val configured: Boolean
    suspend fun snapshot(listId: String): SharedSnapshot
    suspend fun listIds(): List<String>
    suspend fun execute(requestId: String, command: SharedCommand)
    suspend fun executeImport(requestId: String, command: SharedCommand.ImportFilms): ImportBatchResult
    suspend fun preview(token: String): InvitePreview
    suspend fun createInvite(listId: String): String
    suspend fun activity(listId: String, before: Long? = null): ActivityPage
    /** Invalidation only; consumers must fetch authoritative snapshots after reconnecting. */
    fun invalidations(): Flow<Unit>
}

class SharedAccessDeniedException : IllegalStateException("You no longer have access to this shared list.")
class SharingUnavailableException : IllegalStateException("Sharing is not configured. See the backend setup guide.")
class SharedConnectionException : IllegalStateException("Shared changes need a connection. Reconnect and retry.")
class SharedPendingException : IllegalStateException("A previous change is being checked. Reconnect and retry.")

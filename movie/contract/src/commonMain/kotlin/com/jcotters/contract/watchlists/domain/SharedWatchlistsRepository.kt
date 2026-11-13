package com.jcotters.contract.watchlists.domain

import com.jcotters.contract.watchlists.data.FilmActivity
import com.jcotters.contract.watchlists.data.InvitePreview
import kotlinx.coroutines.flow.Flow

private const val INVITATION_TOKEN_LENGTH = 64

data class SharedSyncStatus(val refreshing: Boolean = false, val checkedAt: Long? = null, val error: String? = null)

interface ISharedWatchlistsRepository {
    val syncStatus: kotlinx.coroutines.flow.StateFlow<SharedSyncStatus>
    val configured: Boolean
    fun observeActivity(listId: String): Flow<List<FilmActivity>>
    suspend fun refresh(listId: String? = null): Result<Unit>
    suspend fun publish(listId: String): Result<Unit>
    suspend fun preview(invitation: String): Result<InvitePreview>
    suspend fun join(invitation: String): Result<String>
    suspend fun invite(listId: String): Result<String>
    suspend fun revokeInvites(listId: String): Result<Unit>
    suspend fun leave(listId: String): Result<Unit>
    suspend fun loadActivity(listId: String, older: Boolean = false): Result<Boolean>
}

object WatchlistInvitation {
    fun token(value: String): String {
        val token = value.trim().removePrefix("movers://join/")
        require(token.length == INVITATION_TOKEN_LENGTH && token.all { it in '0'..'9' || it in 'a'..'f' }) {
            "Enter a valid Movers invitation link."
        }
        return token
    }

    fun url(token: String): String = "movers://join/${token(token)}"
}

interface InvitationSharing {
    fun share(url: String)
}

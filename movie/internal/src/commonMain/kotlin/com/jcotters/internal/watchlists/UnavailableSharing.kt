package com.jcotters.internal.watchlists

import com.jcotters.contract.watchlists.data.ActivityPage
import com.jcotters.contract.watchlists.data.InvitePreview
import com.jcotters.contract.watchlists.data.SharedCommand
import com.jcotters.contract.watchlists.data.SharedSnapshot
import com.jcotters.contract.watchlists.data.SharedWatchlistRemote
import com.jcotters.contract.watchlists.data.SharingUnavailableException
import com.jcotters.profile.domain.ParticipantIdentity
import kotlinx.coroutines.flow.emptyFlow

internal object UnavailableSharing : SharedWatchlistRemote, ParticipantIdentity {
    override val configured = false
    override suspend fun currentId(): String? = null
    override suspend fun ensure(name: String, color: Int): String = throw SharingUnavailableException()
    override suspend fun update(name: String, color: Int): Unit = throw SharingUnavailableException()
    override suspend fun snapshot(listId: String): SharedSnapshot = throw SharingUnavailableException()
    override suspend fun listIds(): List<String> = throw SharingUnavailableException()
    override suspend fun execute(requestId: String, command: SharedCommand): Unit = throw SharingUnavailableException()
    override suspend fun preview(token: String): InvitePreview = throw SharingUnavailableException()
    override suspend fun createInvite(listId: String): String = throw SharingUnavailableException()
    override suspend fun activity(listId: String, before: Long?): ActivityPage = throw SharingUnavailableException()
    override fun invalidations() = emptyFlow<Unit>()
}

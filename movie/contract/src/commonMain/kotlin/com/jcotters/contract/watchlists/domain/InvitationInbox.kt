package com.jcotters.contract.watchlists.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Incoming links stay in memory and outside persisted navigation state. Joining is always explicit. */
object InvitationInbox {
    private val pending = MutableStateFlow<String?>(null)
    val invitation = pending.asStateFlow()
    fun offer(url: String): Boolean {
        if (!url.startsWith("movers://join/")) return false
        pending.value = url
        return true
    }
    fun consume(value: String) { pending.compareAndSet(value, null) }
}

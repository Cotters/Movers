package com.jcotters.movers.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class WatchlistNotificationTarget(val listId: String, val groupId: String)

internal object IncomingWatchlistNotification {
    private val pending = MutableStateFlow<WatchlistNotificationTarget?>(null)
    val target = pending.asStateFlow()
    private val uuidPattern = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

    fun offer(version: Int, type: String, listId: String, groupId: String): Boolean {
        val validPayload = version == 1 && type == "watchlist_additions"
        val validIdentifiers = uuidPattern.matches(listId) && uuidPattern.matches(groupId)
        if (!validPayload || !validIdentifiers) return false
        pending.value = WatchlistNotificationTarget(listId.lowercase(), groupId.lowercase())
        return true
    }

    fun consume(target: WatchlistNotificationTarget) {
        pending.update { if (it == target) null else it }
    }
}

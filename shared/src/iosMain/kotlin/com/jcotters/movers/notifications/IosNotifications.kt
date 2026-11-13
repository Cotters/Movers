package com.jcotters.movers.notifications

import com.jcotters.contract.watchlists.data.PushEnvironment
import com.jcotters.profile.domain.NotificationPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object IosNotifications : NativeNotifications {
    private val device = MutableStateFlow(NativeNotificationState())
    override val state = device.asStateFlow()
    var request: () -> Unit = {}
    var settings: () -> Unit = {}
    var update: () -> Unit = {}
    override fun requestPermission() = request()
    override fun openSettings() = settings()
    override fun refresh() = update()

    fun receive(authorization: String, deviceId: String, environment: String, token: String?, error: String?) {
        val permission = when (authorization) {
            "allowed" -> NotificationPermission.Allowed
            "denied" -> NotificationPermission.Denied
            else -> NotificationPermission.NotDetermined
        }
        val pushEnvironment = PushEnvironment.entries.firstOrNull { it.wireName == environment }
        device.value = NativeNotificationState(permission, deviceId, pushEnvironment, token, error)
    }
}

/** Installed by the native host; callbacks and status are delivered on the main thread. */
fun configureIosNotifications(requestPermission: () -> Unit, openSettings: () -> Unit, refresh: () -> Unit) {
    IosNotifications.request = requestPermission
    IosNotifications.settings = openSettings
    IosNotifications.update = refresh
}

fun receiveIosNotificationStatus(
    authorization: String,
    deviceId: String,
    environment: String,
    token: String?,
    error: String?,
) = IosNotifications.receive(authorization, deviceId, environment, token, error)

fun receiveWatchlistNotification(version: Int, type: String, listId: String, groupId: String): Boolean =
    IncomingWatchlistNotification.offer(version, type, listId, groupId)

package com.jcotters.movers.notifications

import com.jcotters.contract.watchlists.data.PushEnvironment
import com.jcotters.profile.domain.NotificationPermission
import kotlinx.coroutines.flow.StateFlow

internal data class NativeNotificationState(
    val permission: NotificationPermission = NotificationPermission.NotDetermined,
    val deviceId: String? = null,
    val environment: PushEnvironment? = null,
    val token: String? = null,
    val error: String? = null,
)

internal interface NativeNotifications {
    val state: StateFlow<NativeNotificationState>
    fun requestPermission()
    fun openSettings()
    fun refresh()
}

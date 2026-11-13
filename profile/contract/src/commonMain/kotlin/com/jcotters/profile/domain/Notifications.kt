package com.jcotters.profile.domain

import kotlinx.coroutines.flow.StateFlow

/** Platform notification policy; native handles and push tokens never enter screen state. */
enum class NotificationPermission { Unsupported, NotDetermined, Denied, Allowed }

data class NotificationSettings(
    val permission: NotificationPermission = NotificationPermission.Unsupported,
    val registering: Boolean = false,
    val registered: Boolean = false,
    val error: String? = null,
)

interface Notifications {
    val settings: StateFlow<NotificationSettings>
    fun sharedListUsed()
    fun requestPermission()
    fun openSettings()
    fun refresh()
}

package com.jcotters.movers.notifications

import com.jcotters.contract.result.recoverableResult
import com.jcotters.contract.watchlists.data.PushRegistration
import com.jcotters.contract.watchlists.data.PushRegistrations
import com.jcotters.profile.domain.NotificationPermission
import com.jcotters.profile.domain.NotificationSettings
import com.jcotters.profile.domain.Notifications
import com.jcotters.profile.domain.ParticipantIdentity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class WatchlistNotifications(
    private val native: NativeNotifications,
    private val registrations: PushRegistrations,
    private val identity: ParticipantIdentity,
) : Notifications {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val state = MutableStateFlow(NotificationSettings())
    override val settings = state.asStateFlow()
    private val refreshes = MutableStateFlow(0)
    private var permissionRequested = false

    init {
        scope.launch {
            combine(native.state, refreshes) { device, _ -> device }.collectLatest { reconcile(it) }
        }
    }

    override fun sharedListUsed() {
        if (native.state.value.permission == NotificationPermission.NotDetermined && !permissionRequested) {
            permissionRequested = true
            native.requestPermission()
        }
        refresh()
    }

    override fun requestPermission() = native.requestPermission()
    override fun openSettings() = native.openSettings()

    override fun refresh() {
        native.refresh()
        refreshes.update { it + 1 }
    }

    private suspend fun reconcile(device: NativeNotificationState) {
        state.value = NotificationSettings(permission = device.permission, error = device.error)
        val registration = registration(device) ?: return
        if (!registrations.configured) {
            state.update { it.copy(error = "Sharing is not configured.") }
        } else {
            state.update { it.copy(registering = true) }
            try {
                recoverableResult { registerExistingParticipant(registration) }
                    .onSuccess { registered ->
                        state.update { it.copy(registered = registered && registration.enabled) }
                    }
                    .onFailure {
                        state.update {
                            it.copy(error = "Couldn't update notifications. Check your connection and retry.")
                        }
                    }
            } finally {
                state.update { it.copy(registering = false) }
            }
        }
    }

    private fun registration(device: NativeNotificationState): PushRegistration? {
        val enabled = device.permission == NotificationPermission.Allowed
        return when {
            device.error != null -> null
            device.permission == NotificationPermission.NotDetermined -> null
            device.permission == NotificationPermission.Unsupported -> null
            device.environment == null || device.deviceId == null -> null
            enabled && device.token == null -> null
            else -> PushRegistration(device.deviceId, device.environment, device.token, enabled)
        }
    }

    private suspend fun registerExistingParticipant(registration: PushRegistration): Boolean {
        if (identity.currentId() == null) {
            state.update { it.copy(error = "Join or share a watchlist to receive notifications.") }
            return false
        }
        registrations.registerPush(registration)
        return true
    }

    fun close() = scope.cancel()
}

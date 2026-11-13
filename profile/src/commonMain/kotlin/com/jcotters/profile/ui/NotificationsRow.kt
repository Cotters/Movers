package com.jcotters.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jcotters.designsystem.MoversTextButton
import com.jcotters.profile.domain.NotificationPermission
import com.jcotters.profile.domain.NotificationSettings

@Composable
internal fun NotificationsRow(
    settings: NotificationSettings,
    onEnable: () -> Unit,
    onSettings: () -> Unit,
    onRetry: () -> Unit,
) {
    if (settings.permission == NotificationPermission.Unsupported) return
    val undetermined = settings.permission == NotificationPermission.NotDetermined
    val description = when {
        undetermined -> "Get updates when someone adds films to a shared watchlist."
        settings.permission == NotificationPermission.Denied -> "Off in iOS Settings"
        settings.registering -> "Connecting…"
        settings.error != null -> "Needs attention"
        settings.registered -> "On"
        else -> "Waiting for push registration…"
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("Notifications", style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
            MoversTextButton(onClick = if (undetermined) onEnable else onSettings) {
                Text(if (undetermined) "Enable" else "Open Settings")
            }
        }
        settings.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
            MoversTextButton(onClick = onRetry, enabled = !settings.registering) { Text("Retry") }
        }
    }
}

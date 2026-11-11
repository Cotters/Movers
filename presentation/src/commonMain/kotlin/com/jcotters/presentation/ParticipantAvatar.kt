package com.jcotters.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

data class ParticipantAvatarColor(val label: String, val background: Color)

// Indices are persisted and shared. Keep this order stable.
val participantAvatarColors = listOf(
    ParticipantAvatarColor("Blue", Color(0xFF385F9D)),
    ParticipantAvatarColor("Purple", Color(0xFF79519B)),
    ParticipantAvatarColor("Red", Color(0xFF9A414A)),
    ParticipantAvatarColor("Green", Color(0xFF496E3C)),
    ParticipantAvatarColor("Gold", Color(0xFF826019)),
    ParticipantAvatarColor("Teal", Color(0xFF236C73)),
    ParticipantAvatarColor("Orange", Color(0xFF8C4D25)),
    ParticipantAvatarColor("Grey", Color(0xFF645D74)),
)

@Composable
fun ParticipantAvatar(name: String, color: Int, modifier: Modifier = Modifier) {
    com.jcotters.designsystem.MoversAvatar(
        name = name,
        modifier = modifier,
        background = participantAvatarColors[color.mod(participantAvatarColors.size)].background,
        contentColor = Color.White,
    )
}

expect fun formatActivityTime(millis: Long): String
expect fun formatActivityDay(millis: Long): String

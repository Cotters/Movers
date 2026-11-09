package com.jcotters.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val avatarColors = listOf(0xFF385F9D,0xFF79519B,0xFF9A414A,0xFF496E3C,0xFF826019,0xFF236C73,0xFF8C4D25,0xFF645D74)

@Composable
fun ParticipantAvatar(name: String, color: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(32.dp).background(Color(avatarColors[color.mod(avatarColors.size)]), CircleShape)
        .semantics { contentDescription = name }, contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase().ifEmpty { "?" }, color = Color.White)
    }
}

expect fun formatActivityTime(millis: Long): String
expect fun formatActivityDay(millis: Long): String

package com.jcotters.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jcotters.presentation.participantAvatarColors

@Composable
internal fun AvatarColorPicker(selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    LazyRow(Modifier.selectableGroup()) {
        itemsIndexed(participantAvatarColors) { index, color ->
            Box(
                modifier = Modifier.size(48.dp)
                    .selectable(
                        selected = selected == index,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelect(index) },
                    )
                    .semantics { contentDescription = color.label },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(40.dp)
                        .border(2.dp, if (selected == index) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                        .padding(4.dp)
                        .background(color.background, CircleShape),
                )
            }
        }
    }
}

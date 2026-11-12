package com.jcotters.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.jcotters.designsystem.MoversIconButton
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.add
import com.jcotters.presentation.resources.trash
import org.jetbrains.compose.resources.painterResource

@Composable
fun MovieMembershipButton(
    movieTitle: String,
    isAdded: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    saving: Boolean = false,
) {
    val color = if (isAdded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val description = if (isAdded) "Remove $movieTitle from list" else "Add $movieTitle to list"
    MoversIconButton(
        onClick = onClick,
        enabled = enabled && !saving,
        modifier = Modifier.size(48.dp).semantics {
            contentDescription = description
            if (saving) stateDescription = "Saving"
        },
        contentColor = color,
    ) {
        Icon(painterResource(if (isAdded) Res.drawable.trash else Res.drawable.add), contentDescription = null)
    }
}

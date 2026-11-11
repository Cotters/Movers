package com.jcotters.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jcotters.designsystem.MoversButton
import com.jcotters.profile.domain.Profile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewState: ProfileViewState,
    onRetry: () -> Unit = {},
    onSaveName: (String) -> Unit = {},
    savingName: Boolean = false,
    nameError: String? = null,
    onSaveColor: (Int) -> Unit = {},
    savingColor: Boolean = false,
    colorError: String? = null,
    onOpenStyleGuide: (() -> Unit)? = null,
) {
    var editing by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var name by androidx.compose.runtime.saveable.rememberSaveable(viewState.profile?.displayName) { androidx.compose.runtime.mutableStateOf(viewState.profile?.displayName.orEmpty()) }
    Scaffold(topBar = { com.jcotters.designsystem.MoversTopBar(title = { Text("Profile") }) }) { innerPadding ->
        if (viewState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            ProfileView(
                modifier = Modifier.padding(innerPadding),
                viewState = viewState,
                onOpenStyleGuide = onOpenStyleGuide,
                onRetry = onRetry,
                onEditName = { editing = true },
                onSaveColor = onSaveColor,
                savingColor = savingColor,
                colorError = colorError,
                savingName = savingName,
            )
        }
    }
    if (editing) com.jcotters.designsystem.MoversAlertDialog(onDismissRequest = { if (!savingName) editing = false },
        title = { Text("Your name") }, text = { Column {
            com.jcotters.designsystem.MoversTextField(value = name,onValueChange = { name = it },enabled = !savingName,singleLine = true)
            nameError?.let { Text(it,color = MaterialTheme.colorScheme.error) }
        } }, confirmButton = { com.jcotters.designsystem.MoversTextButton(onClick = { onSaveName(name) }, enabled = !savingName && name.isNotBlank()) { Text(if (savingName) "Saving…" else "Save") } },
        dismissButton = { com.jcotters.designsystem.MoversTextButton(onClick = { editing = false },enabled = !savingName) { Text("Close") } })
}

@Composable
fun ProfileView(
    modifier: Modifier,
    viewState: ProfileViewState,
    onRetry: () -> Unit,
    onEditName: () -> Unit = {},
    onSaveColor: (Int) -> Unit = {},
    savingColor: Boolean = false,
    colorError: String? = null,
    savingName: Boolean = false,
    onOpenStyleGuide: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        viewState.profile?.let {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp),verticalAlignment = Alignment.CenterVertically) {
                com.jcotters.presentation.ParticipantAvatar(it.displayName,it.color)
                ProfileDetailsView(it, Modifier.weight(1f))
                com.jcotters.designsystem.MoversTextButton(onClick = onEditName, enabled = !savingName && !savingColor) { Text("Edit name") }
            }
            Text("Avatar colour", style = MaterialTheme.typography.titleSmall)
            AvatarColorPicker(selected = it.color, enabled = !savingName && !savingColor, onSelect = onSaveColor)
            if (savingColor) LinearProgressIndicator(Modifier.fillMaxWidth())
            colorError?.let { message -> Text(message, color = MaterialTheme.colorScheme.error) }
        }

        onOpenStyleGuide?.let { open ->
            Text("Developer tools", style = MaterialTheme.typography.titleSmall)
            com.jcotters.designsystem.MoversTextButton(onClick = open) { Text("Style guide") }
        }

        viewState.errorMessage?.let { message ->
            Text(message)
            MoversButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun ProfileDetailsView(
    profile: Profile,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = profile.displayName,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            style = MaterialTheme.typography.headlineLarge,
        )
    }
}

@Preview
@Composable
private fun ProfilePreview() {
    com.jcotters.designsystem.MoversDesignTheme {
        ProfileScreen(
            viewState = ProfileViewState(
                isLoading = false,
                profile = Profile("preview", "Josh"),
            ),
        )
    }
}

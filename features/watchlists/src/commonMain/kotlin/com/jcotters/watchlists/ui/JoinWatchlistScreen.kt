package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.contract.watchlists.domain.InvitationInbox
import com.jcotters.navigation.Navigator
import com.jcotters.watchlists.SharingViewModel
import com.jcotters.watchlists.navigation.WatchlistDetailKey
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JoinWatchlistRoute(navigator: Navigator) {
    val viewModel: SharingViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val incoming by InvitationInbox.invitation.collectAsStateWithLifecycle()
    LaunchedEffect(incoming, state.busy) {
        incoming?.let { value ->
            if (!state.busy) {
                viewModel.openInvitation(value)
                InvitationInbox.consume(value)
            }
        }
    }
    LaunchedEffect(state.joinedId) {
        state.joinedId?.let {
            viewModel.consumeJoined()
            navigator.goBack()
            navigator.navigate(WatchlistDetailKey(it))
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Join list") },navigationIcon = { TextButton(onClick = navigator::goBack) { Text("Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val openedFromLink = incoming != null || state.openedFromLink
            if (!openedFromLink) {
                OutlinedTextField(value = state.invitation,onValueChange = viewModel::changeInvitation,label = { Text("Invitation link") },enabled = !state.busy,singleLine = true,modifier = Modifier.fillMaxWidth())
            }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (openedFromLink && state.error == null) Text("Joining…")
            state.error?.let { Text(it,color = MaterialTheme.colorScheme.error) }
            if (!openedFromLink || state.error != null) {
                Button(onClick = viewModel::join,enabled = !state.busy && state.invitation.isNotBlank()) {
                    Text(if (openedFromLink) "Retry" else if (state.busy) "Joining…" else "Join list")
                }
            }
            if (openedFromLink && state.error != null) {
                TextButton(onClick = viewModel::editInvitation, enabled = !state.busy) { Text("Use another link") }
            }
        }
    }
}

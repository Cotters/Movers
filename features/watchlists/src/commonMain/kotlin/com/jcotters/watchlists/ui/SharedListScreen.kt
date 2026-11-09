package com.jcotters.watchlists.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.contract.watchlists.domain.InvitationSharing
import com.jcotters.details.navigation.MovieDetailsKey
import com.jcotters.navigation.Navigator
import com.jcotters.presentation.ParticipantAvatar
import com.jcotters.presentation.formatActivityDay
import com.jcotters.presentation.formatActivityTime
import com.jcotters.watchlists.SharingViewModel
import com.jcotters.watchlists.navigation.SharedSection
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SharedListRoute(listId: String, section: SharedSection, navigator: Navigator) {
    val viewModel: SharingViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sharing: InvitationSharing = koinInject()
    var revoking by rememberSaveable { mutableStateOf(false) }
    var publishing by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(listId) { viewModel.load(listId, section == SharedSection.Activity) }
    LaunchedEffect(section, state.list?.shared?.revision) {
        if (section == SharedSection.Activity && !state.busy) viewModel.activity(listId)
    }
    LaunchedEffect(state.inviteUrl) { state.inviteUrl?.let { sharing.share(it); viewModel.consumeInvite() } }
    Scaffold(topBar = { TopAppBar(title = { Text(section.name) }, navigationIcon = { TextButton(onClick = navigator::goBack) { Text("Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
            state.message?.let { Text(it, Modifier.padding(16.dp)) }
            when (section) {
                SharedSection.Members -> LazyColumn {
                    state.list?.shared?.members?.let { members -> items(members, key = { it.id }) { person ->
                        ListItem(headlineContent = { Text(person.name) }, leadingContent = { ParticipantAvatar(person.name,person.color) },
                            supportingContent = { Text(if (person.id == state.list?.shared?.creator?.id) "Creator · member" else "Member · equal editing access") })
                    } }
                }
                SharedSection.Activity -> {
                    TextButton(onClick = { viewModel.activity(listId) }, enabled = !state.busy) { Text("Refresh activity") }
                    LazyColumn(Modifier.weight(1f)) {
                        state.events.groupBy { formatActivityDay(it.at) }.forEach { (day, events) ->
                            item(key = "day-$day") { Text(day,Modifier.padding(16.dp),style = MaterialTheme.typography.titleSmall) }
                            items(events,key = { it.id }) { event ->
                                ListItem(leadingContent = { ParticipantAvatar(event.actor.name,event.actor.color) },
                                    headlineContent = { Text("${event.actor.name} ${if (event.imported) "imported" else if (event.added) "added" else "removed"} ${event.film.title}") },
                                    supportingContent = { Text(formatActivityTime(event.at)) },
                                    modifier = Modifier.clickable { navigator.navigate(MovieDetailsKey(event.film.id)) })
                            }
                        }
                        if (state.events.isEmpty()) item { Text("No cached film activity yet. Refresh when connected.",Modifier.padding(16.dp)) }
                        if (state.moreActivity && state.events.isNotEmpty()) item { TextButton(onClick = { viewModel.activity(listId,true) }, enabled = !state.busy) { Text("Load older activity") } }
                    }
                }
                SharedSection.Sharing -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.list?.shared == null) {
                        Text("Share this list with friends. Everyone who joins can rename it, add or remove films, and invite others. Shared edits require a connection.")
                        Text("Your device holds your sharing identity. If it is lost, a friend will need to invite your new identity.")
                        Button(onClick = { publishing = true }, enabled = !state.busy && state.list != null) { Text("Share with friends") }
                    } else {
                        Text("Everyone has equal controls. Invitations work for seven days; anyone holding a valid link can join.")
                        Button(onClick = { viewModel.invite(listId) }, enabled = !state.busy) { Text("Invite friends") }
                        TextButton(onClick = { revoking = true }, enabled = !state.busy) { Text("Revoke outstanding invitations") }
                        Text("There is no device recovery in this version. Keep an existing member on the list if you replace your device.")
                    }
                }
            }
        }
    }
    if (publishing) AlertDialog(onDismissRequest = { publishing = false },title = { Text("Make this list collaborative?") },
        text = { Text("Its films will be uploaded. Friends who join can edit and invite others. Existing films will be attributed to you as imported when shared.") },
        confirmButton = { TextButton(onClick = { publishing = false; viewModel.publish(listId) }) { Text("Share list") } },
        dismissButton = { TextButton(onClick = { publishing = false }) { Text("Cancel") } })
    if (revoking) AlertDialog(onDismissRequest = { revoking = false },title = { Text("Revoke all invitation links?") },
        text = { Text("Existing members keep access. You can create a new link afterwards.") },
        confirmButton = { TextButton(onClick = { revoking = false; viewModel.revoke(listId) }) { Text("Revoke links") } },
        dismissButton = { TextButton(onClick = { revoking = false }) { Text("Cancel") } })
}

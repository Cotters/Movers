package com.jcotters.watchlists.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.contract.watchlists.data.FilmActivity
import com.jcotters.contract.watchlists.domain.InvitationSharing
import com.jcotters.contract.watchlists.domain.Watchlist
import com.jcotters.designsystem.MoversAlertDialog
import com.jcotters.designsystem.MoversButton
import com.jcotters.designsystem.MoversListItem
import com.jcotters.designsystem.MoversTextButton
import com.jcotters.designsystem.MoversTopBar
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
    var revoking by rememberSaveable(listId) { mutableStateOf(false) }
    LaunchedEffect(listId, section) {
        viewModel.load(listId, includeActivity = section == SharedSection.Activity, sharingRequested = section == SharedSection.Sharing)
    }
    LaunchedEffect(state.inviteUrl) {
        state.inviteUrl?.let { sharing.share(it); viewModel.consumeInvite() }
    }
    Scaffold(topBar = {
        MoversTopBar(
            title = { Text(if (section == SharedSection.Sharing) "Share list" else section.name) },
            navigationIcon = { MoversTextButton(onClick = navigator::goBack) { Text("Back") } },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let {
                Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                MoversTextButton(onClick = {
                    viewModel.load(listId, includeActivity = section == SharedSection.Activity,
                        sharingRequested = section == SharedSection.Sharing)
                }, enabled = !state.busy) { Text("Retry") }
            }
            state.message?.let { Text(it, Modifier.padding(16.dp)) }
            if (!state.busy && state.error == null && state.list == null) {
                Text("This list no longer exists.", Modifier.padding(16.dp))
            }
            when (section) {
                SharedSection.Members -> state.list?.let { SharedListMembers(it) }
                SharedSection.Activity -> SharedListActivity(
                    events = state.events,
                    more = state.moreActivity,
                    busy = state.busy,
                    onRefresh = { viewModel.activity(listId) },
                    onLoadOlder = { viewModel.activity(listId, older = true) },
                    onOpenMovie = { navigator.navigate(MovieDetailsKey(it)) },
                    modifier = Modifier.weight(1f),
                )
                SharedSection.Sharing -> state.list?.let { list ->
                    SharingActions(list, state.busy,
                        onShare = { viewModel.invite(listId) }, onRevoke = { revoking = true })
                }
            }
        }
    }
    if (revoking) MoversAlertDialog(
        onDismissRequest = { revoking = false },
        title = { Text("Revoke all links?") },
        text = { Text("Existing members keep access.") },
        confirmButton = {
            MoversTextButton(onClick = { revoking = false; viewModel.revoke(listId) }, enabled = !state.busy,
                destructive = true) { Text("Revoke") }
        },
        dismissButton = { MoversTextButton(onClick = { revoking = false }) { Text("Cancel") } },
    )
}

@Composable
private fun SharedListMembers(list: Watchlist) {
    LazyColumn {
        list.shared?.members?.let { members ->
            items(members, key = { it.id }) { person ->
                MoversListItem(
                    headlineContent = { Text(person.name) },
                    leadingContent = { ParticipantAvatar(person.name, person.color) },
                    supportingContent = { Text(if (person.id == list.shared?.creator?.id) "Creator" else "Member") },
                )
            }
        }
    }
}

@Composable
private fun SharedListActivity(
    events: List<FilmActivity>,
    more: Boolean,
    busy: Boolean,
    onRefresh: () -> Unit,
    onLoadOlder: () -> Unit,
    onOpenMovie: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = com.jcotters.designsystem.MoversDesign.tokens.colors
    Column(modifier) {
        MoversTextButton(onClick = onRefresh, enabled = !busy) { Text("Refresh activity") }
        LazyColumn(Modifier.weight(1f)) {
            events.groupBy { formatActivityDay(it.at) }.forEach { (day, dayEvents) ->
                item(key = "day-$day") { Text(day, Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall) }
                items(dayEvents, key = { it.id }) { event ->
                    val action = if (!event.added) "removed" else if (event.imported) "imported" else "added"
                    MoversListItem(
                        leadingContent = { ParticipantAvatar(event.actor.name, event.actor.color) },
                        headlineContent = {
                            Text(buildAnnotatedString {
                                append("${event.actor.name} ")
                                withStyle(SpanStyle(color = if (event.added) colors.success else colors.danger)) {
                                    append(action)
                                }
                                append(" ")
                                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                    append(event.film.title)
                                }
                            })
                        },
                        supportingContent = { Text(formatActivityTime(event.at)) },
                        modifier = Modifier.clickable { onOpenMovie(event.film.id) },
                    )
                }
            }
            if (events.isEmpty() && !busy) item { Text("No activity yet.", Modifier.padding(16.dp)) }
            if (more && events.isNotEmpty()) item {
                MoversTextButton(onClick = onLoadOlder, enabled = !busy) { Text("Load older activity") }
            }
        }
    }
}

@Composable
private fun SharingActions(list: Watchlist, busy: Boolean, onShare: () -> Unit, onRevoke: () -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(list.name, style = MaterialTheme.typography.headlineSmall)
        Text("Anyone with the link can join and edit. Links expire in 7 days.")
        MoversButton(onClick = onShare, enabled = !busy && list.shared != null) { Text("Share link") }
        if (list.shared != null) MoversTextButton(onClick = onRevoke, enabled = !busy,
            destructive = true) { Text("Revoke links") }
    }
}

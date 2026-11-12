package com.jcotters.watchlists.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jcotters.contract.watchlists.data.FilmContribution
import com.jcotters.contract.watchlists.data.SharedListInfo
import com.jcotters.designsystem.MoversIconButton
import com.jcotters.presentation.ParticipantAvatar

@Composable
internal fun WatchlistSummary(
    movieCount: Int,
    sharedInfo: SharedListInfo?,
    onMembers: () -> Unit,
    modifier: Modifier = Modifier,
    totalMovieCount: Int = movieCount,
    sort: com.jcotters.watchlists.preferences.WatchlistSort = com.jcotters.watchlists.preferences.WatchlistSort(),
    sortEnabled: Boolean = false,
    onSort: (com.jcotters.watchlists.preferences.WatchlistSort) -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (movieCount == totalMovieCount) "$movieCount ${if (movieCount == 1) "movie" else "movies"}"
            else "$movieCount of $totalMovieCount ${if (totalMovieCount == 1) "movie" else "movies"}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WatchlistSortMenu(sort, sortEnabled, onSort)
        if (sharedInfo != null) {
            val memberCount = sharedInfo.members.size
            Row(
                modifier = Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button, onClickLabel = "View members", onClick = onMembers)
                    .semantics { contentDescription = "$memberCount ${if (memberCount == 1) "member" else "members"}" }
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                sharedInfo.members.take(3).forEach { person -> ParticipantAvatar(person.name, person.color) }
                val remaining = memberCount - 3
                if (remaining > 0) Text("+$remaining", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
internal fun FilmContributorButton(
    contribution: FilmContribution,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MoversIconButton(
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = "${if (contribution.imported) "Imported" else "Added"} by ${contribution.actor.name}"
        },
    ) {
        Box(Modifier.clearAndSetSemantics {}) {
            Surface(
                shape = CircleShape,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
            ) { ParticipantAvatar(contribution.actor.name, contribution.actor.color) }
        }
    }
}

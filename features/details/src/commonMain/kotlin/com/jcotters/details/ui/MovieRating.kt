package com.jcotters.details.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import kotlin.math.round

@Composable
internal fun MovieRating(score: Double, voteCount: Int, modifier: Modifier = Modifier) {
    val value = round(score * 10) / 10
    val votes = if (voteCount == 1) "1 vote" else "$voteCount votes"
    Column(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "TMDB rating: $value out of 10, $votes"
        },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            com.jcotters.designsystem.MoversRatingStars(score = score, modifier = Modifier.size(78.dp, 14.dp))
            Text("$value/10", style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "TMDB · $votes",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

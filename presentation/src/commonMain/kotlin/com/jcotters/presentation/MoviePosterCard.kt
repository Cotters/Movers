package com.jcotters.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jcotters.contract.detail.domain.models.Movie

@Composable
fun MoviePosterCard(
    movie: Movie,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    posterModifier: Modifier = Modifier,
    posterOverlay: @Composable BoxScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen)) {
        Box {
            MoverImageView(
                posterUrl = movie.posterUrl.orEmpty(),
                contentDescription = "Movie poster for ${movie.title}",
                sharedImageKey = moviePosterKey(movie.id),
                modifier = posterModifier.fillMaxWidth().aspectRatio(2f / 3f),
            )
            posterOverlay()
        }
        Text(
            movie.title,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (movie.releaseDate.isNotBlank()) {
            Text(
                movie.releaseDate.take(4),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

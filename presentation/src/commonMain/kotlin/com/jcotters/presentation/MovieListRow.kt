package com.jcotters.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.add
import org.jetbrains.compose.resources.painterResource

@Composable
fun MovieListRow(
    movie: Movie,
    modifier: Modifier = Modifier,
    posterModifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = if (onOpen == null) modifier else modifier.clickable(onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoverImageView(
            posterUrl = movie.posterUrl.orEmpty(),
            sharedImageKey = moviePosterKey(movie.id),
            contentDescription = "Movie poster for ${movie.title}",
            modifier = posterModifier.fillMaxHeight().aspectRatio(9f / 16f).padding(vertical = 8.dp),
        )
        Column(Modifier.weight(1f).padding(8.dp)) {
            Text(movie.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(movie.synopsis, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
        trailingContent()
    }
}

@Composable
fun AddToListButton(onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
        Icon(painterResource(Res.drawable.add), contentDescription = "Add to list")
    }
}

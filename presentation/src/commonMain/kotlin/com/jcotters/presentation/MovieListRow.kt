package com.jcotters.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.designsystem.MoversIconButton
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.add
import org.jetbrains.compose.resources.painterResource

@Composable
fun MovieListRow(
    movie: Movie,
    modifier: Modifier = Modifier,
    posterModifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
    compact: Boolean = false,
    posterWidth: Dp? = null,
    posterOverlay: @Composable BoxScope.() -> Unit = {},
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    com.jcotters.designsystem.MoversFilmRow(
        title = movie.title,
        metadata = movie.releaseDate.take(4),
        synopsis = movie.synopsis,
        onOpen = onOpen,
        compact = compact,
        posterWidth = posterWidth,
        modifier = modifier.padding(vertical = 8.dp),
        poster = { imageModifier ->
            Box(imageModifier) {
                MoverImageView(
                    posterUrl = movie.posterUrl.orEmpty(),
                    sharedImageKey = moviePosterKey(movie.id),
                    contentDescription = "Movie poster for ${movie.title}",
                    modifier = posterModifier.fillMaxSize(),
                )
                posterOverlay()
            }
        },
        trailingContent = trailingContent,
    )
}

@Composable
fun AddToListButton(onClick: () -> Unit, enabled: Boolean = true) {
    MoversIconButton(onClick = onClick, enabled = enabled,
        contentColor = MaterialTheme.colorScheme.primary) {
        Icon(painterResource(Res.drawable.add), contentDescription = "Add to list")
    }
}

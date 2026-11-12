package com.jcotters.presentation

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jcotters.contract.detail.domain.models.Movie

@Composable
fun MoviePosterCard(
    movie: Movie,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    posterModifier: Modifier = Modifier,
    posterOverlay: @Composable BoxScope.() -> Unit = {},
    footerContent: (@Composable () -> Unit)? = null,
) {
    com.jcotters.designsystem.MoversFilmCard(
        title = movie.title,
        year = movie.releaseDate.take(4),
        onOpen = onOpen,
        modifier = modifier,
        poster = { imageModifier ->
            MoverImageView(
                posterUrl = movie.posterUrl.orEmpty(),
                contentDescription = "Movie poster for ${movie.title}",
                sharedImageKey = moviePosterKey(movie.id),
                modifier = posterModifier.then(imageModifier),
            )
        },
        posterOverlay = posterOverlay,
        footerContent = footerContent,
    )
}

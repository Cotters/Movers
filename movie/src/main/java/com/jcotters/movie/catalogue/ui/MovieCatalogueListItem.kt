package com.jcotters.movie.catalogue.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jcotters.movie.detail.domain.models.Movie
import com.jcotters.movie.detail.ui.MoverImageView

@Composable
fun MovieCatalogueListItem(
  movie: Movie,
  modifier: Modifier = Modifier,
) {
  Row(modifier = modifier) {
    MoverImageView(
      posterUrl = movie.posterUrl.orEmpty(),
      contentDescription = "Movie poster for ${movie.title}",
      modifier = Modifier
        .fillMaxHeight()
        .aspectRatio(9f / 16f)
    )

    Column(
      modifier = Modifier
        .fillMaxHeight()
        .padding(all = 8.dp),
      verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterVertically)
    ) {
      Text(text = movie.title, style = MaterialTheme.typography.titleLarge)
      Text(
        text = movie.synopsis,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Preview
@Composable
private fun MovieCatalogueListItemPreview() {
  MovieCatalogueListItem(
    movie = Movie(id = 1, title = "Preview: The Movie", synopsis = "A thrilling preview!", releaseDate = "Today"),
  )
}
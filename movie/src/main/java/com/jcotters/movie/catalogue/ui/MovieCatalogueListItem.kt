package com.jcotters.movie.catalogue.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jcotters.movie.detail.domain.models.Movie
import com.jcotters.movie.detail.ui.MoverImageView

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MovieCatalogueListItem(
  movie: Movie,
  modifier: Modifier = Modifier,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
) {
  Row(modifier = modifier) {
    sharedTransitionScope.MoverImageView(
      animatedVisibilityScope = animatedVisibilityScope,
      posterUrl = movie.posterUrl.orEmpty(),
      contentDescription = "DbMovie poster for ${movie.title}",
      modifier = Modifier
        .fillMaxHeight()
        .aspectRatio(9f / 16f)
        .padding(horizontal = 0.dp, vertical = 8.dp)
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(
  widthDp = 800,
  heightDp = 200,
)
@Composable
private fun MovieCatalogueListItemPreview() {
  SharedTransitionLayout {
    AnimatedVisibility(visible = true) {
      Surface {
        MovieCatalogueListItem(
          sharedTransitionScope = this@SharedTransitionLayout,
          animatedVisibilityScope = this,
          movie = Movie(
            id = 1,
            title = "Preview: The DbMovie",
            synopsis = "A thrilling preview!",
            releaseDate = "Today"
          ),
        )
      }
    }
  }
}
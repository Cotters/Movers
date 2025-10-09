package com.jcotters.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jcotters.movie.detail.domain.models.PreviewMovies
import com.jcotters.movie.detail.ui.MoverImageView
import com.jcotters.profile.domain.Profile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
  viewState: ProfileViewState,
  onViewEvent: (ProfileViewEvent) -> Unit,
) {
  Scaffold { innerPadding ->
    if (viewState.isLoading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else {
      ProfileView(
        modifier = Modifier.padding(innerPadding),
        viewState = viewState,
        onViewEvent = onViewEvent,
      )
    }
  }
}

@Composable
fun ProfileView(
  modifier: Modifier,
  viewState: ProfileViewState,
  onViewEvent: (ProfileViewEvent) -> Unit,
) {
  Column(
    modifier = modifier
      .padding(horizontal = 8.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    viewState.profile?.let {
      ProfileDetailsView(it)
    }

    Text(text = "Bookmarks", style = MaterialTheme.typography.headlineMedium)

    LazyRow(
      modifier = Modifier
        .height(250.dp)
        .fillMaxWidth()
        .background(color = MaterialTheme.colorScheme.surfaceContainer),
    ) {
      if (viewState.bookmarkedMovies.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillParentMaxWidth()
              .fillParentMaxHeight(),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "No Bookmarked Moovers",
              style = MaterialTheme.typography.headlineSmall,
            )
          }
        }
      }
      items(viewState.bookmarkedMovies) { movie ->
        MoverImageView(
          posterUrl = movie.posterUrl.orEmpty(),
          contentDescription = "Movie poster for ${movie.title}",
          modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(9f / 16f)
        )
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    Button(
      onClick = { onViewEvent(ProfileViewEvent.LogoutTapped) },
      modifier = Modifier
        .fillMaxWidth()
        .padding(all = 8.dp),
      shape = RoundedCornerShape(8.dp),
    ) {
      Text("Logout")
    }
  }
}

@Composable
fun ProfileDetailsView(
  profile: Profile,
) {
  Column {
    Text(
      text = profile.username,
      style = MaterialTheme.typography.headlineLarge,
    )
  }
}

@Preview
@Composable
private fun NoBookmarksPreview() {
  ProfileScreen(
    viewState = ProfileViewState(
      isLoading = false,
      profile = Profile(username = "Preview Account"),
      bookmarkedMovies = emptyList(),
    ),
    onViewEvent = {},
  )
}

@Preview
@Composable
private fun BookmarksPreview() {
  ProfileScreen(
    viewState = ProfileViewState(
      isLoading = false,
      profile = Profile(username = "Preview Account"),
      bookmarkedMovies = PreviewMovies.movies.take(1),
    ),
    onViewEvent = {},
  )
}
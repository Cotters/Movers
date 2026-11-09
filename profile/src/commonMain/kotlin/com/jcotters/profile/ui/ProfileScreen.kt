package com.jcotters.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.contract.detail.domain.models.PreviewMovies
import com.jcotters.presentation.MoverImageView
import com.jcotters.presentation.moviePosterKey
import com.jcotters.profile.domain.Profile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewState: ProfileViewState,
    onRetry: () -> Unit = {},
    onSaveName: (String) -> Unit = {},
    savingName: Boolean = false,
    nameError: String? = null,
    posterModifier: @Composable (Movie) -> Modifier,
    onMovieTapped: (Int) -> Unit,
) {
    var editing by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var name by androidx.compose.runtime.saveable.rememberSaveable(viewState.profile?.displayName) { androidx.compose.runtime.mutableStateOf(viewState.profile?.displayName.orEmpty()) }
    Scaffold { innerPadding ->
        if (viewState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            ProfileView(
                modifier = Modifier.padding(innerPadding),
                viewState = viewState,
                onRetry = onRetry,
                onEditName = { editing = true },
                posterModifier = posterModifier,
                onMovieTapped = onMovieTapped,
            )
        }
    }
    if (editing) androidx.compose.material3.AlertDialog(onDismissRequest = { if (!savingName) editing = false },
        title = { Text("Your name") }, text = { Column {
            androidx.compose.material3.OutlinedTextField(value = name,onValueChange = { name = it },enabled = !savingName,singleLine = true)
            nameError?.let { Text(it,color = MaterialTheme.colorScheme.error) }
        } }, confirmButton = { androidx.compose.material3.TextButton(onClick = { onSaveName(name) }, enabled = !savingName && name.isNotBlank()) { Text(if (savingName) "Saving…" else "Save") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { editing = false },enabled = !savingName) { Text("Close") } })
}

@Composable
fun ProfileView(
    modifier: Modifier,
    viewState: ProfileViewState,
    onRetry: () -> Unit,
    onEditName: () -> Unit = {},
    posterModifier: @Composable (Movie) -> Modifier,
    onMovieTapped: (Int) -> Unit,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        viewState.profile?.let {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp),verticalAlignment = Alignment.CenterVertically) {
                com.jcotters.presentation.ParticipantAvatar(it.displayName,it.color)
                ProfileDetailsView(it)
                androidx.compose.material3.TextButton(onClick = onEditName) { Text("Edit name") }
            }
        }

        viewState.errorMessage?.let { message ->
            Text(message)
            Button(onClick = onRetry) { Text("Retry") }
        }

        Text(text = "Bookmarks", style = MaterialTheme.typography.headlineMedium)

        LazyRow(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surfaceContainer),
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            text = "No Saved Movies",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }
            }
            items(viewState.bookmarkedMovies) { movie ->
                MoverImageView(
                    posterUrl = movie.posterUrl.orEmpty(),
                    sharedImageKey = moviePosterKey(movie.id),
                    contentDescription = "Movie poster for ${movie.title}",
                    modifier = posterModifier(movie)
                        .fillMaxHeight()
                        .aspectRatio(3f / 5f)
                        .clickable { onMovieTapped(movie.id) }
                )
            }
        }

    }
}

@Composable
fun ProfileDetailsView(
    profile: Profile,
) {
    Column {
        Text(
            text = profile.displayName,
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
            profile = Profile("preview", "Josh"),
            bookmarkedMovies = emptyList(),
        ),
        posterModifier = { Modifier },
        onMovieTapped = {},
    )
}

@Preview
@Composable
private fun BookmarksPreview() {
    ProfileScreen(
        viewState = ProfileViewState(
            isLoading = false,
            profile = Profile("preview", "Josh"),
            bookmarkedMovies = PreviewMovies.movies.take(1),
        ),
        posterModifier = { Modifier },
        onMovieTapped = {},
    )
}

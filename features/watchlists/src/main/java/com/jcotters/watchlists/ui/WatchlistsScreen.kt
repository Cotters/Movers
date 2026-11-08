package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.features.watchlists.R

@Composable
internal fun WatchlistsRoute() {
    val viewModel: WatchlistsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WatchlistsScreen(state)
}

@Composable
internal fun WatchlistsScreen(state: WatchlistsViewState) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(stringResource(R.string.watchlists_title), style = MaterialTheme.typography.headlineMedium)
            when (state) {
                WatchlistsViewState.Loading -> CircularProgressIndicator()
                WatchlistsViewState.AuthRequired -> Text(stringResource(R.string.watchlists_auth_required))
                WatchlistsViewState.ComingSoon -> Text(stringResource(R.string.watchlists_coming_soon))
            }
        }
    }
}

@Preview
@Composable
private fun GuestWatchlistsPreview() {
    MaterialTheme { WatchlistsScreen(WatchlistsViewState.AuthRequired) }
}

@Preview
@Composable
private fun AuthenticatedWatchlistsPreview() {
    MaterialTheme { WatchlistsScreen(WatchlistsViewState.ComingSoon) }
}

package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jcotters.features.watchlists.resources.Res
import com.jcotters.features.watchlists.resources.watchlists_coming_soon
import com.jcotters.features.watchlists.resources.watchlists_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchlistsRoute() {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(stringResource(Res.string.watchlists_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(Res.string.watchlists_coming_soon))
        }
    }
}

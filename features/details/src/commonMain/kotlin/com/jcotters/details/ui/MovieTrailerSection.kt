package com.jcotters.details.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun MovieTrailerSection(movieId: Int) {
    val viewModel: MovieTrailerViewModel = koinViewModel { parametersOf(movieId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var expanded by rememberSaveable(movieId) { mutableStateOf(false) }
    var linkError by remember(movieId) { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var active by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> active = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!expanded) {
            TextButton(onClick = { expanded = true; viewModel.load() }) { Text("Watch trailer") }
        } else {
            val trailer = state.trailer
            when {
                state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error != null -> {
                    Text(checkNotNull(state.error), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::load) { Text("Retry") }
                }
                trailer != null -> {
                    Text(trailer.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (active) {
                        key(trailer.embedUrl) {
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                TrailerPlayer(trailer.embedUrl, Modifier.fillMaxWidth().height((maxWidth * 9f / 16f).coerceAtLeast(200.dp)))
                            }
                        }
                    }
                }
                state.loaded -> Text("No trailer available.")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (trailer != null) {
                    TextButton(onClick = {
                        try {
                            uriHandler.openUri(trailer.watchUrl)
                            linkError = null
                        } catch (_: Exception) {
                            linkError = "Couldn't open the trailer."
                        }
                    }) { Text("Open in YouTube") }
                }
                TextButton(onClick = { expanded = false; linkError = null }) { Text("Close") }
            }
            linkError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
internal expect fun TrailerPlayer(embedUrl: String, modifier: Modifier)

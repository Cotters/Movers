package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.designsystem.MoversButton
import com.jcotters.designsystem.MoversIconButton
import com.jcotters.designsystem.MoversTextButton
import com.jcotters.designsystem.MoversTopBar
import com.jcotters.navigation.Navigator
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.arrow_back
import com.jcotters.watchlists.ListTransferViewModel
import com.jcotters.watchlists.navigation.FilmImportKey
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListTransferRoute(listId: String, navigator: Navigator) {
    val viewModel: ListTransferViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(listId) { viewModel.load(listId) }

    Scaffold(topBar = {
        MoversTopBar(
            title = { Text("Import / export") },
            navigationIcon = {
                MoversIconButton(onClick = navigator::goBack) {
                    Icon(painterResource(Res.drawable.arrow_back), "Back")
                }
            },
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                state.loading -> LinearProgressIndicator()
                state.error != null -> {
                    Text(checkNotNull(state.error), color = MaterialTheme.colorScheme.error)
                    MoversTextButton(onClick = { viewModel.load(listId) }) { Text("Retry") }
                }
                state.content == null -> Text("This list no longer exists.")
                else -> {
                    val content = checkNotNull(state.content)
                    Text(content.watchlist.name, style = MaterialTheme.typography.titleMedium)
                    MoversButton(onClick = { navigator.navigate(FilmImportKey(listId)) }) { Text("Import films") }
                    HorizontalDivider()
                    Text("Export list", style = MaterialTheme.typography.titleMedium)
                    ExportListControls(state, viewModel::changeFormat, viewModel::export,
                        viewModel::documentDispatched, viewModel::documentResult)
                }
            }
        }
    }
}

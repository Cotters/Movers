package com.jcotters.watchlists.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.designsystem.MoversButton
import com.jcotters.designsystem.MoversCard
import com.jcotters.designsystem.MoversCheckbox
import com.jcotters.designsystem.MoversDropdownMenu
import com.jcotters.designsystem.MoversDropdownMenuItem
import com.jcotters.designsystem.MoversFilterChip
import com.jcotters.designsystem.MoversIconButton
import com.jcotters.designsystem.MoversModalSheet
import com.jcotters.designsystem.MoversTextButton
import com.jcotters.designsystem.MoversTextField
import com.jcotters.designsystem.MoversTopBar
import com.jcotters.navigation.Navigator
import com.jcotters.presentation.MovieSearchField
import com.jcotters.presentation.MovieSearchResult
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.arrow_back
import com.jcotters.watchlists.FilmImportState
import com.jcotters.watchlists.FilmImportViewModel
import com.jcotters.watchlists.ImportRowState
import com.jcotters.watchlists.documents.InputFormat
import com.jcotters.watchlists.documents.rememberDocumentActions
import com.jcotters.watchlists.documents.rememberImportHelpPreference
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilmImportRoute(listId: String, navigator: Navigator) {
    val viewModel: FilmImportViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(listId) { viewModel.load(listId) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, viewModel) {
        val observer =
            LifecycleEventObserver { _, _ -> viewModel.active(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); viewModel.active(false) }
    }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus()
        keyboard?.hide()
    }
    val preference = rememberImportHelpPreference()
    var help by rememberSaveable { mutableStateOf(!preference.seen) }
    val documents = rememberDocumentActions(
        onPicked = { viewModel.pickedFile(it.name, it.bytes) },
        onResult = viewModel::documentResult,
    )
    LaunchedEffect(state.remainingDocument) {
        state.remainingDocument?.let { document ->
            viewModel.remainingDocumentDispatched()
            documents.save(document)
        }
    }
    Scaffold(modifier = Modifier.pointerInput(focusManager, keyboard) {
        detectTapGestures(onTap = { dismissKeyboard() })
    }, topBar = {
        MoversTopBar(title = { Text("Import films") }, navigationIcon = {
            MoversIconButton(onClick = { viewModel.pause(); navigator.goBack() }) {
                Icon(
                    painterResource(Res.drawable.arrow_back),
                    "Back"
                )
            }
        }, actions = { MoversTextButton(onClick = { help = !help }) { Text("Help") } })
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(state.listName, style = MaterialTheme.typography.titleMedium) }
            if (help) item {
                MoversCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Choose a file or paste titles. We'll add matches and flag anything not found.")
                        Row(
                            horizontalArrangement = Arrangement.End,
                        ) {
                            MoversTextButton(onClick = { preference.dismiss(); help = false }) { Text("Got it") }
                        }
                    }
                }
            }
            state.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
            state.documentMessage?.let { message -> item { Text(message) } }
            if (!state.started) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MoversButton(
                            onClick = { dismissKeyboard(); documents.open() },
                            enabled = !state.parsing
                        ) { Text("Choose file") }
                        state.filename?.let {
                            Text(
                                it,
                                Modifier.padding(start = 12.dp).weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (state.filename == null) item {
                    MoversTextField(
                        value = state.text,
                        onValueChange = viewModel::sourceChanged,
                        label = { Text("Paste titles or CSV") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 7,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                    )
                } else item { MoversTextButton(onClick = { viewModel.sourceChanged("") }) { Text("Paste instead") } }
                item { ImportFormatControls(state, viewModel) }
                if (state.parsing) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                state.parsed?.let { parsed ->
                    item { Text("${parsed.films.size} entries · ${parsed.format.label}") }
                    items(parsed.films.take(5)) { film ->
                        Column {
                            Text(
                                film.reference.title.ifBlank { film.original }.take(200),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            film.reference.year?.let { Text(it.toString(), style = MaterialTheme.typography.bodySmall) }
                            film.issue?.let {
                                Text(
                                    it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                item {
                    MoversButton(
                        onClick = { dismissKeyboard(); viewModel.start() },
                        enabled = !state.parsing && !state.unavailable && !state.parsed?.films.isNullOrEmpty()
                    ) { Text("Import") }
                }
            } else {
                item {
                    Text(
                        "${state.importedCount} imported · ${state.skippedCount} skipped · ${state.remainingCount} ${if (state.running) "remaining" else "need attention"}",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                if (state.running) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.running) MoversTextButton(onClick = viewModel::pause) { Text("Stop") }
                        else if (state.canResume) {
                            MoversButton(onClick = viewModel::start) { Text(if (state.error != null) "Retry failed" else "Resume") }
                        }
                        if (state.remainingCount > 0) MoversTextButton(
                            onClick = viewModel::saveRemaining,
                            enabled = !state.running && !state.savingRemaining
                        ) { Text("Save remaining") }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MoversFilterChip(
                            selected = !state.showAllRows,
                            onClick = { viewModel.showAllRows(false) },
                            label = { Text("Needs attention") })
                        MoversFilterChip(
                            selected = state.showAllRows,
                            onClick = { viewModel.showAllRows(true) },
                            label = { Text("All") })
                    }
                }
                items(state.visibleRows, key = { it.id }) { row ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(row.displayTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                row.statusLabel, style = MaterialTheme.typography.bodySmall,
                                color = if (row.remaining && !state.running) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (row.remaining && row.status != ImportRowState.Pending) MoversTextButton(
                            onClick = { viewModel.openResolution(row.id) },
                            enabled = state.canResolve,
                        ) { Text("Find film") }
                    }
                }
                if (!state.running) item { MoversButton(onClick = navigator::goBack) { Text("Done") } }
            }
        }
    }
    if (state.resolving != null) MoversModalSheet(onDismissRequest = viewModel::closeResolution) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Find film", style = MaterialTheme.typography.titleLarge)
            MovieSearchField(state.query, viewModel::queryChanged, onSearch = viewModel::search)
            if (state.searching) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.searchError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!state.searching && state.candidates.isEmpty()) Text("No matches. Try another title.")
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(state.candidates, key = { it.id }) { movie ->
                    MovieSearchResult(movie, compact = true) {
                        MoversTextButton(onClick = { keyboard?.hide(); viewModel.select(movie) }) { Text("Import") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportFormatControls(state: FilmImportState, viewModel: FilmImportViewModel) {
    var menu by remember { mutableStateOf(false) }
    var columns by rememberSaveable { mutableStateOf(false) }
    Column {
        Row {
            MoversDropdownMenu(expanded = menu, onExpandedChange = { menu = it }, anchor = {
                MoversTextButton(onClick = { menu = !menu }) { Text("Format: ${state.format.label}") }
            }) {
                InputFormat.entries.forEach { format ->
                    MoversDropdownMenuItem(
                        text = { Text(format.label) },
                        onClick = { menu = false; viewModel.formatChanged(format) })
                }
            }
            if (state.parsed?.headings?.isNotEmpty() == true) MoversTextButton(onClick = { columns = !columns }) {
                Text(
                    "Columns"
                )
            }
        }
        val parsed = state.parsed
        if (columns && parsed != null && parsed.headings.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoversCheckbox(state.headerOverride ?: parsed.hasHeader, onCheckedChange = viewModel::headerChanged)
                Text("Header row")
            }
            val mapping = state.mappingOverride ?: parsed.mapping
            ColumnPicker("Title", parsed.headings, mapping.title) { viewModel.mappingChanged(mapping.copy(title = it)) }
            ColumnPicker("Year", parsed.headings, mapping.year) { viewModel.mappingChanged(mapping.copy(year = it)) }
            ColumnPicker(
                "TMDB ID",
                parsed.headings,
                mapping.movieId
            ) { viewModel.mappingChanged(mapping.copy(movieId = it)) }
            ColumnPicker(
                "IMDb ID",
                parsed.headings,
                mapping.imdbId
            ) { viewModel.mappingChanged(mapping.copy(imdbId = it)) }
        }
    }
}

@Composable
private fun ColumnPicker(label: String, headings: List<String>, selected: Int?, onSelect: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row {
        MoversDropdownMenu(expanded, onExpandedChange = { expanded = it }, anchor = {
            MoversTextButton(onClick = {
                expanded = !expanded
            }) { Text("$label: ${selected?.let { headings.getOrNull(it) } ?: "None"}") }
        }) {
            MoversDropdownMenuItem(text = { Text("None") }, onClick = { expanded = false; onSelect(null) })
            headings.forEachIndexed { index, heading ->
                MoversDropdownMenuItem(
                    text = { Text(heading) },
                    onClick = { expanded = false; onSelect(index) })
            }
        }
    }
}

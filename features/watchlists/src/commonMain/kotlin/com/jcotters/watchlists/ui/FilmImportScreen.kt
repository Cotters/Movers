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
import androidx.compose.foundation.lazy.LazyListScope
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
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.watchlists.FilmImportInput
import com.jcotters.watchlists.FilmImportSession
import com.jcotters.watchlists.ImportRow
import com.jcotters.watchlists.MAX_IMPORT_TITLE_LENGTH
import com.jcotters.watchlists.documents.DocumentActions
import com.jcotters.watchlists.documents.ExportDocument
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
    LaunchedEffect(listId) { viewModel.session.load(listId) }
    ImportSessionLifecycle(viewModel.session)
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus()
        keyboard?.hide()
    }
    val preference = rememberImportHelpPreference()
    var help by rememberSaveable { mutableStateOf(!preference.seen) }
    val documents = rememberImportDocuments(viewModel, state.remainingDocument)
    Scaffold(modifier = Modifier.pointerInput(focusManager, keyboard) {
        detectTapGestures(onTap = { dismissKeyboard() })
    }, topBar = {
        MoversTopBar(title = { Text("Import films") }, navigationIcon = {
            MoversIconButton(onClick = { viewModel.session.pause(); navigator.goBack() }) {
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
                ImportHelpCard(onDismiss = { preference.dismiss(); help = false })
            }
            state.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
            state.documentMessage?.let { message -> item { Text(message) } }
            if (!state.started) {
                importSource(
                    state, viewModel.input,
                    onChooseFile = { dismissKeyboard(); documents.open() },
                    onStart = { dismissKeyboard(); viewModel.session.start() },
                    onDismissKeyboard = dismissKeyboard,
                )
            } else {
                importProgress(state, viewModel.session::pause, viewModel.session::start, viewModel::saveRemaining)
                importResultRows(state, viewModel::showAllRows, viewModel::openResolution, navigator::goBack)

            }
        }
    }
    if (state.resolving != null) FilmResolutionSheet(
        state, viewModel::queryChanged, viewModel::search,
        onSelect = { keyboard?.hide(); viewModel.select(it) }, onDismiss = viewModel::closeResolution,
    )
}

@Composable
private fun ImportFormatControls(state: FilmImportState, input: FilmImportInput) {
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
                        onClick = { menu = false; input.formatChanged(format) })
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
                MoversCheckbox(
                    state.headerOverride ?: parsed.hasHeader,
                    onCheckedChange = input::headerChanged
                )
                Text("Header row")
            }
            val mapping = state.mappingOverride ?: parsed.mapping
            ColumnPicker(
                "Title",
                parsed.headings,
                mapping.title
            ) { input.mappingChanged(mapping.copy(title = it)) }
            ColumnPicker(
                "Year",
                parsed.headings,
                mapping.year
            ) { input.mappingChanged(mapping.copy(year = it)) }
            ColumnPicker(
                "TMDB ID",
                parsed.headings,
                mapping.movieId
            ) { input.mappingChanged(mapping.copy(movieId = it)) }
            ColumnPicker(
                "IMDb ID",
                parsed.headings,
                mapping.imdbId
            ) { input.mappingChanged(mapping.copy(imdbId = it)) }
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

private const val IMPORT_PREVIEW_ROWS = 5
private const val PASTE_MIN_LINES = 4
private const val PASTE_MAX_LINES = 7

private fun LazyListScope.importSource(
    state: FilmImportState,
    input: FilmImportInput,
    onChooseFile: () -> Unit,
    onStart: () -> Unit,
    onDismissKeyboard: () -> Unit,
) {
    item {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MoversButton(
                onClick = onChooseFile,
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
            onValueChange = input::sourceChanged,
            label = { Text("Paste titles or CSV") },
            modifier = Modifier.fillMaxWidth(),
            minLines = PASTE_MIN_LINES,
            maxLines = PASTE_MAX_LINES,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDismissKeyboard() }),
        )
    } else item { MoversTextButton(onClick = { input.sourceChanged("") }) { Text("Paste instead") } }
    item { ImportFormatControls(state, input) }
    if (state.parsing) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
    state.parsed?.let { parsed ->
        item { Text("${parsed.films.size} entries · ${parsed.format.label}") }
        items(parsed.films.take(IMPORT_PREVIEW_ROWS)) { film ->
            Column {
                Text(
                    film.reference.title.ifBlank { film.original }.take(MAX_IMPORT_TITLE_LENGTH),
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
            onClick = onStart,
            enabled = !state.parsing && !state.unavailable && !state.parsed?.films.isNullOrEmpty()
        ) { Text("Import") }
    }
}

private fun LazyListScope.importProgress(
    state: FilmImportState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSaveRemaining: () -> Unit,
) {
    item {
        Text(
            state.progressLabel,
            style = MaterialTheme.typography.titleSmall
        )
    }
    if (state.running) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.running) MoversTextButton(onClick = onPause) { Text("Stop") }
            else if (state.canResume) {
                MoversButton(onClick = onResume) {
                    Text(if (state.error != null) "Retry failed" else "Resume")
                }
            }
            if (state.remainingCount > 0) MoversTextButton(
                onClick = onSaveRemaining,
                enabled = !state.running && !state.savingRemaining
            ) { Text("Save remaining") }
        }
    }
}

private fun LazyListScope.importResultRows(
    state: FilmImportState,
    onShowAll: (Boolean) -> Unit,
    onResolve: (Int) -> Unit,
    onDone: () -> Unit,
) {
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MoversFilterChip(
                selected = !state.showAllRows,
                onClick = { onShowAll(false) },
                label = { Text("Needs attention") })
            MoversFilterChip(
                selected = state.showAllRows,
                onClick = { onShowAll(true) },
                label = { Text("All") })
        }
    }
    items(state.visibleRows, key = { it.id }) { row ->
        ImportResultRow(row, state.running, state.canResolve, onResolve)
    }
    if (!state.running) item { MoversButton(onClick = onDone) { Text("Done") } }
}

@Composable
private fun ImportResultRow(row: ImportRow, running: Boolean, canResolve: Boolean, onResolve: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(row.displayTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                row.statusLabel, style = MaterialTheme.typography.bodySmall,
                color = if (row.remaining && !running) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (row.remaining && row.status != ImportRowState.Pending) MoversTextButton(
            onClick = { onResolve(row.id) },
            enabled = canResolve,
        ) { Text("Find film") }
    }
}

@Composable
private fun FilmResolutionSheet(
    state: FilmImportState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSelect: (Movie) -> Unit,
    onDismiss: () -> Unit,
) {
    MoversModalSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Find film", style = MaterialTheme.typography.titleLarge)
            MovieSearchField(state.query, onQueryChange, onSearch = onSearch)
            if (state.searching) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.searchError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!state.searching && state.candidates.isEmpty()) Text("No matches. Try another title.")
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(state.candidates, key = { it.id }) { movie ->
                    MovieSearchResult(movie, compact = true) {
                        MoversTextButton(onClick = { onSelect(movie) }) { Text("Import") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportSessionLifecycle(session: FilmImportSession) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, session) {
        val observer =
            LifecycleEventObserver { _, _ ->
                session.active(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
            }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            session.active(false)
        }
    }

}

@Composable
private fun rememberImportDocuments(
    viewModel: FilmImportViewModel,
    remainingDocument: ExportDocument?
): DocumentActions {
    val documents = rememberDocumentActions(
        onPicked = { viewModel.input.pickedFile(it.name, it.bytes) },
        onResult = viewModel::documentResult,
    )
    LaunchedEffect(remainingDocument) {
        remainingDocument?.let { document ->
            viewModel.remainingDocumentDispatched()
            documents.save(document)
        }
    }

    return documents
}

@Composable
private fun ImportHelpCard(onDismiss: () -> Unit) {
    MoversCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Choose a file or paste titles. We'll add matches and flag anything not found.")
            Row(
                horizontalArrangement = Arrangement.End,
            ) {
                MoversTextButton(onClick = onDismiss) { Text("Got it") }
            }
        }
    }
}

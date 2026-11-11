package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jcotters.contract.watchlists.domain.WatchlistContent
import com.jcotters.watchlists.documents.DocumentResult
import com.jcotters.watchlists.documents.ExportFormat
import com.jcotters.watchlists.documents.ListDocuments
import com.jcotters.watchlists.documents.rememberDocumentActions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExportListSheet(content: WatchlistContent, onDismiss: () -> Unit) {
    var format by rememberSaveable { mutableStateOf(ExportFormat.Csv) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val actions = rememberDocumentActions(onPicked = {}, onResult = { result ->
        busy = false
        message = when (result) {
            DocumentResult.Saved -> "File saved."
            DocumentResult.Shared -> "Share sheet opened."
            DocumentResult.Cancelled -> null
            is DocumentResult.Failed -> result.message
        }
    })
    fun export(share: Boolean) {
        if (busy) return
        busy = true; message = null
        scope.launch {
            try {
                val document = withContext(Dispatchers.Default) { ListDocuments.export(content.watchlist.name, content.movies, format) }
                if (share) actions.share(document) else actions.save(document)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { busy = false; message = "Couldn't export this list." }
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Export list", style = MaterialTheme.typography.titleLarge)
            Text(content.watchlist.name)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportFormat.entries.forEach { option -> FilterChip(selected = format == option, onClick = { format = option }, enabled = !busy, label = { Text(option.label) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { export(false) }, enabled = !busy) { Text("Save file") }
                TextButton(onClick = { export(true) }, enabled = !busy) { Text("Share") }
            }
            message?.let { Text(it) }
        }
    }
}

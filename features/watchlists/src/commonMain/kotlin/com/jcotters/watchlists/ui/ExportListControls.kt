package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jcotters.designsystem.MoversButton
import com.jcotters.designsystem.MoversFilterChip
import com.jcotters.designsystem.MoversTextButton
import com.jcotters.watchlists.ListTransferState
import com.jcotters.watchlists.documents.DocumentResult
import com.jcotters.watchlists.documents.ExportFormat
import com.jcotters.watchlists.documents.rememberDocumentActions

@Composable
internal fun ExportListControls(
    state: ListTransferState,
    onFormat: (ExportFormat) -> Unit,
    onExport: (Boolean) -> Unit,
    onDispatched: () -> Unit,
    onResult: (DocumentResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = rememberDocumentActions(onPicked = {}, onResult = onResult)
    LaunchedEffect(state.document) {
        state.document?.let { document ->
            onDispatched()
            if (state.shareDocument) actions.share(document) else actions.save(document)
        }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExportFormat.entries.forEach { option ->
                MoversFilterChip(selected = state.format == option, onClick = { onFormat(option) },
                    enabled = !state.exporting, label = { Text(option.label) })
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MoversButton(onClick = { onExport(false) }, enabled = !state.exporting) { Text("Save file") }
            MoversTextButton(onClick = { onExport(true) }, enabled = !state.exporting) { Text("Share") }
        }
        state.exportMessage?.let { Text(it) }
    }
}

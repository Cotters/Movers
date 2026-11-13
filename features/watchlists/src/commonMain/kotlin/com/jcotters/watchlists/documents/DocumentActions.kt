package com.jcotters.watchlists.documents

import androidx.compose.runtime.Composable

internal data class PickedDocument(val name: String, val bytes: ByteArray)
internal sealed interface DocumentResult {
    data object Saved : DocumentResult
    data object Shared : DocumentResult
    data object Cancelled : DocumentResult
    data class Failed(val message: String) : DocumentResult
}

internal interface DocumentActions {
    fun open()
    fun save(document: ExportDocument)
    fun share(document: ExportDocument)
}

@Composable
internal expect fun rememberDocumentActions(
    onPicked: (PickedDocument) -> Unit,
    onResult: (DocumentResult) -> Unit
): DocumentActions

internal interface ImportHelpPreference {
    val seen: Boolean
    fun dismiss()
}

@Composable
internal expect fun rememberImportHelpPreference(): ImportHelpPreference

internal val DocumentResult.message: String?
    get() = when (this) {
        DocumentResult.Saved -> "File saved."
        DocumentResult.Shared -> "Share sheet opened."
        DocumentResult.Cancelled -> null
        is DocumentResult.Failed -> message
    }

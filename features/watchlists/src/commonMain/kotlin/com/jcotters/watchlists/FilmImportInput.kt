package com.jcotters.watchlists

import com.jcotters.contract.result.recoverableResult
import com.jcotters.watchlists.documents.ColumnMapping
import com.jcotters.watchlists.documents.InputFormat
import com.jcotters.watchlists.documents.ListDocuments
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PARSE_DEBOUNCE_MILLIS = 250L

internal class FilmImportInput(
    private val state: MutableStateFlow<FilmImportState>,
    private val scope: CoroutineScope
) {
    private var source = ""
    private var parser: Job? = null

    fun sourceChanged(text: String) {
        if (state.value.started) return
        source = text
        state.update { it.copy(text = text, filename = null) }
        parse()
    }

    fun pickedFile(name: String, bytes: ByteArray) {
        if (state.value.started) return
        parser?.cancel()
        state.update { it.copy(parsing = true, error = null) }
        parser = scope.launch {
            recoverableResult {
                source = withContext(Dispatchers.Default) { ListDocuments.decode(bytes) }
                state.update {
                    it.copy(
                        filename = name,
                        text = "",
                        format = InputFormat.Auto,
                        headerOverride = null,
                        mappingOverride = null
                    )
                }
                parseNow()
            }.onFailure { e ->
                state.update { it.copy(parsing = false, parsed = null, error = e.message ?: "Unable to read file.") }
            }
        }
    }

    fun formatChanged(format: InputFormat) {
        state.update { it.copy(format = format, mappingOverride = null, headerOverride = null) }
        parse()
    }

    fun headerChanged(header: Boolean) {
        state.update { it.copy(headerOverride = header, mappingOverride = null) }
        parse()
    }

    fun mappingChanged(mapping: ColumnMapping) {
        state.update { it.copy(mappingOverride = mapping) }
        parse()
    }

    private fun parse() {
        parser?.cancel()
        state.update { it.copy(parsing = true, error = null) }
        parser = scope.launch {
            delay(PARSE_DEBOUNCE_MILLIS)
            parseNow()
        }
    }

    private suspend fun parseNow() {
        val current = state.value
        recoverableResult {
            val result = withContext(Dispatchers.Default) {
                ListDocuments.parse(
                    source,
                    current.format,
                    current.headerOverride,
                    current.mappingOverride
                )
            }
            state.update { it.copy(parsing = false, parsed = result, error = null) }
        }.onFailure { e ->
            state.update { it.copy(parsing = false, parsed = null, error = e.message ?: "Unable to read these films.") }
        }
    }
}

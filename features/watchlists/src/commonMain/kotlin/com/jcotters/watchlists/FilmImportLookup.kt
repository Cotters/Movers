package com.jcotters.watchlists

import com.jcotters.contract.result.recoverableResult
import com.jcotters.contract.watchlists.data.FilmReference
import com.jcotters.contract.watchlists.data.FilmResolution
import com.jcotters.contract.watchlists.data.MAX_IMPORT_BATCH_SIZE
import com.jcotters.contract.watchlists.data.MovieImportResolver
import com.jcotters.contract.watchlists.data.MovieLookupException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

private const val LOOKUP_RETRY_MILLIS = 1000L

internal class FilmImportLookup(
    private val resolver: MovieImportResolver,
    private val state: MutableStateFlow<FilmImportState>,
) {
    private data class LookupResult(
        val rows: List<ImportRow>,
        val resolution: FilmResolution?,
        val issue: String?,
    )

    private val matches = mutableMapOf<FilmReference, FilmResolution>()

    suspend fun resolveReady(): Boolean {
        var lookupFailed = false
        val next = state.value.rows.filter { it.status == ImportRowState.Ready }.take(MAX_IMPORT_BATCH_SIZE)
        for (pair in next.groupBy { it.input.reference }.entries.chunked(2)) {
            val results = coroutineScope {
                pair.map { (reference, rows) ->
                    async { resolveGroup(reference, rows) }
                }.map { it.await() }
            }
            results.forEach { result ->
                applyResolution(result)
                if (result.issue != null) lookupFailed = true
            }
            if (lookupFailed) return true
        }

        return false
    }

    private suspend fun resolveGroup(reference: FilmReference, rows: List<ImportRow>): LookupResult {
        rows.forEach { row -> state.updateRow(row.id) { it.copy(status = ImportRowState.LookingUp) } }
        return recoverableResult { resolve(reference) }.fold(
            onSuccess = { LookupResult(rows, it, null) },
            onFailure = { LookupResult(rows, null, it.message ?: "Unable to look up this film.") },
        )
    }

    private fun applyResolution(result: LookupResult) {
        result.rows.forEach { row ->
            state.updateRow(row.id) {
                when (val resolution = result.resolution) {
                    is FilmResolution.Matched -> it.copy(
                        status = ImportRowState.Resolved, movie = resolution.movie, issue = null,
                    )

                    is FilmResolution.Review -> it.copy(
                        status = ImportRowState.Review, candidates = resolution.candidates, issue = resolution.reason,
                    )

                    null -> it.copy(status = ImportRowState.Failed, issue = result.issue)
                }
            }
        }
    }

    private suspend fun resolve(reference: FilmReference): FilmResolution {
        matches[reference]?.let { return it }
        repeat(2) { attempt ->
            try {
                return resolver.resolve(reference).also { matches[reference] = it }
            } catch (e: MovieLookupException) {
                if (attempt == 1) throw e
                delay(e.retryAfterMillis ?: LOOKUP_RETRY_MILLIS)
            }
        }
        error("Lookup did not complete")
    }
}

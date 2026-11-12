package com.jcotters.watchlists.preferences

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Installation-local presentation preferences. Never part of a shared-list command/snapshot. */
interface WatchlistPreferenceStorage {
    suspend fun read(): String?
    suspend fun write(value: String)
}

internal const val MaximumPinnedWatchlists = 5

internal data class WatchlistPreferenceState(
    val ready: Boolean = false,
    val pinnedIds: List<String> = emptyList(),
    val sorts: Map<String, WatchlistSort> = emptyMap(),
)

internal interface WatchlistPreferences {
    val state: StateFlow<WatchlistPreferenceState>
    suspend fun load(): Result<Unit>
    suspend fun setSort(listId: String, sort: WatchlistSort): Result<Unit>
    suspend fun setPinned(listId: String, pinned: Boolean): Result<Unit>
    suspend fun retainLists(listIds: Set<String>): Result<Unit>
}

/** Serializes read/modify/write operations so concurrent pins cannot exceed the cap. */
internal class LocalWatchlistPreferences(private val storage: WatchlistPreferenceStorage) : WatchlistPreferences {
    private val mutableState = MutableStateFlow(WatchlistPreferenceState())
    override val state = mutableState.asStateFlow()
    private val mutex = Mutex()

    override suspend fun load(): Result<Unit> = change { it }

    override suspend fun setSort(listId: String, sort: WatchlistSort): Result<Unit> = change { current ->
        requireAvailable(listId)
        current.copy(sorts = if (sort == WatchlistSort()) current.sorts - listId else current.sorts + (listId to sort))
    }

    override suspend fun setPinned(listId: String, pinned: Boolean): Result<Unit> = change { current ->
        requireAvailable(listId)
        if (pinned && listId !in current.pinnedIds) {
            require(current.pinnedIds.size < MaximumPinnedWatchlists) { "You can pin up to five lists." }
            current.copy(pinnedIds = current.pinnedIds + listId)
        } else if (!pinned) current.copy(pinnedIds = current.pinnedIds - listId) else current
    }

    override suspend fun retainLists(listIds: Set<String>): Result<Unit> = change { current ->
        current.copy(pinnedIds = current.pinnedIds.filter { it in listIds }, sorts = current.sorts.filterKeys { it in listIds })
    }

    private fun requireAvailable(listId: String) {
        require(listId.isNotBlank()) { "This list is no longer available." }
    }

    private suspend fun change(transform: (WatchlistPreferenceState) -> WatchlistPreferenceState): Result<Unit> = mutex.withLock {
        try {
            if (!mutableState.value.ready) mutableState.value = decode(storage.read()).copy(ready = true)
            val current = mutableState.value
            val next = transform(current)
            if (next != current) {
                currentCoroutineContext().ensureActive()
                // Finish a started native write and its state publication as one small critical section.
                withContext(NonCancellable) {
                    storage.write(encode(next))
                    mutableState.value = next
                }
                currentCoroutineContext().ensureActive()
            }
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun decode(value: String?): WatchlistPreferenceState {
        if (value == null) return WatchlistPreferenceState()
        val root = Json.parseToJsonElement(value).jsonObject
        val pins = (root["pins"] as? JsonArray).orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull }
            .filter(String::isNotBlank).distinct().take(MaximumPinnedWatchlists)
        val sorts = root["sorts"]?.jsonObject.orEmpty().mapValues { (_, value) ->
            val setting = value.jsonObject
            val field = WatchlistSortField.entries.firstOrNull { it.name == setting["field"]?.jsonPrimitive?.contentOrNull }
                ?: WatchlistSortField.Added
            WatchlistSort(field, setting["ascending"]?.jsonPrimitive?.booleanOrNull ?: false)
        }
        return WatchlistPreferenceState(pinnedIds = pins, sorts = sorts)
    }

    private fun encode(state: WatchlistPreferenceState): String = buildJsonObject {
        put("pins", JsonArray(state.pinnedIds.map(::JsonPrimitive)))
        put("sorts", buildJsonObject {
            state.sorts.forEach { (id, sort) ->
                put(id, buildJsonObject { put("field", sort.field.name); put("ascending", sort.ascending) })
            }
        })
    }.toString()
}

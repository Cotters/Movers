package com.jcotters.watchlists.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data object JoinWatchlistKey : NavKey

@Serializable
enum class SharedSection { Members, Activity, Sharing }

@Serializable
data class SharedListKey(val listId: String, val section: SharedSection) : NavKey

@Serializable
data class WatchlistDetailKey(val listId: String, val notificationEntryId: String? = null) : NavKey

@Serializable
data class ListTransferKey(val listId: String) : NavKey

@Serializable
data class FilmImportKey(val listId: String, val instanceId: String = Uuid.random().toString()) : NavKey

@Serializable
data class AddToListsKey(
    val movieId: Int,
    val initialListId: String? = null,
    val instanceId: String = Uuid.random().toString()
) : NavKey

package com.jcotters.watchlists.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class WatchlistDetailKey(val listId: String) : NavKey

@Serializable
data class AddToListsKey(val movieId: Int, val initialListId: String? = null, val instanceId: String = Uuid.random().toString()) : NavKey

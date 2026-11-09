package com.jcotters.search.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class MovieSearchKey(val listId: String? = null, val instanceId: String = Uuid.random().toString()) : NavKey

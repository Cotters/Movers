package com.jcotters.details.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class MovieDetailsKey(
    val movieId: Int,
    val instanceId: String = kotlin.uuid.Uuid.random().toString(),
) : NavKey

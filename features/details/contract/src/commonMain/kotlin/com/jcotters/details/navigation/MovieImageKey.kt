package com.jcotters.details.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
enum class MovieImageType { Poster, Backdrop }

@Serializable
data class MovieImageKey(
    val movieId: Int,
    val imageType: MovieImageType,
    val sourceEntryId: String,
) : NavKey

package com.jcotters.database.movies

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movie_catalogue_remote_keys")
data class DbMovieCatalogueRemoteKey(
    @PrimaryKey
    val catalogue: String,
    val nextPage: Int?,
)
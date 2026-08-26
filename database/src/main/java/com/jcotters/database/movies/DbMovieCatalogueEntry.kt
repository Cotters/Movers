package com.jcotters.database.movies

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "movie_catalogue_entries",
    primaryKeys = ["catalogue", "movieId"],
    foreignKeys = [
        ForeignKey(
            entity = DbMovie::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("movieId"),
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("movieId"), Index(value = ["catalogue", "page", "pageIndex"])],
)
data class DbMovieCatalogueEntry(
    val catalogue: String,
    val movieId: Int,
    val page: Int,
    val pageIndex: Int,
)
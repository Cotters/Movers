package com.jcotters.database.watchlists

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.jcotters.database.movies.DbMovie

@Entity(tableName = "watchlists")
data class DbWatchlist(@PrimaryKey val id: String, val name: String, val createdAt: Long)

@Entity(
    tableName = "watchlist_movies",
    primaryKeys = ["watchlistId", "movieId"],
    indices = [Index("movieId")],
    foreignKeys = [
        ForeignKey(entity = DbWatchlist::class, parentColumns = ["id"], childColumns = ["watchlistId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = DbMovie::class, parentColumns = ["id"], childColumns = ["movieId"], onDelete = ForeignKey.NO_ACTION),
    ],
)
data class DbWatchlistMovie(val watchlistId: String, val movieId: Int, val addedAt: Long)

data class DbWatchlistSummary(val id: String, val name: String, val createdAt: Long, val movieCount: Int)

data class WatchlistEntryWithMovie(
    @Embedded val entry: DbWatchlistMovie,
    @Relation(parentColumn = "movieId", entityColumn = "id") val movie: DbMovie,
)

data class WatchlistWithEntries(
    @Embedded val list: DbWatchlist,
    @Relation(parentColumn = "id", entityColumn = "watchlistId", entity = DbWatchlistMovie::class)
    val entries: List<WatchlistEntryWithMovie>,
)

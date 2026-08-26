package com.jcotters.database.movies

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

enum class MovieCatalogue(val key: String) {
    New("new:release_date.desc:v1"),
    Popular("popular:popularity.desc:v1")
}

@Dao
interface MovieCatalogueDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCatalogueEntries(entries: List<DbMovieCatalogueEntry>)

    @Query("DELETE FROM movie_catalogue_entries WHERE catalogue = :catalogue")
    suspend fun removeAllForCatalogue(catalogue: String)
}
package com.jcotters.database.movies

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MovieCatalogueRemoteKeysDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: DbMovieCatalogueRemoteKey)

    @Query("SELECT * from movie_catalogue_remote_keys WHERE catalogue = :catalogue")
    suspend fun getRemoteKey(catalogue: String): DbMovieCatalogueRemoteKey?

    @Query("DELETE FROM movie_catalogue_remote_keys WHERE catalogue = :catalogue")
    suspend fun removeCatalogueKeys(catalogue: String)
}


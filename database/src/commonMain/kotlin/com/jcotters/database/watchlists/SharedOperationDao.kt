package com.jcotters.database.watchlists

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SharedOperationDao {
    @Query("SELECT * FROM shared_operations ORDER BY rowid")
    suspend fun operations(): List<DbSharedOperation>

    @Insert
    suspend fun insert(operation: DbSharedOperation)

    @Query("DELETE FROM shared_operations WHERE id = :id")
    suspend fun complete(id: String)

    @Query("DELETE FROM shared_operations WHERE listId = :listId")
    suspend fun discard(listId: String)
}

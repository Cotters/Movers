package com.jcotters.database.watchlists

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SharedActivityDao {
    @Query("SELECT * FROM shared_activity WHERE listId = :id ORDER BY sequence DESC")
    fun observeActivity(id: String): Flow<List<DbSharedActivity>>

    @Query("SELECT * FROM shared_activity_cursors WHERE listId = :id")
    suspend fun cursor(id: String): DbSharedActivityCursor?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCursor(cursor: DbSharedActivityCursor)

    @Query("DELETE FROM shared_activity_cursors WHERE listId = :id")
    suspend fun clearCursor(id: String)

    @Query("SELECT MIN(sequence) FROM shared_activity WHERE listId = :id")
    suspend fun oldest(id: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun putActivity(events: List<DbSharedActivity>)

    @Query("DELETE FROM shared_activity WHERE listId = :id")
    suspend fun clearActivity(id: String)
}

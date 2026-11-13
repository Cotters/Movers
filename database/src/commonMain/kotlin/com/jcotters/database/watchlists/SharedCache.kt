package com.jcotters.database.watchlists

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "shared_lists")
data class DbSharedList(
    @PrimaryKey val listId: String,
    val participantId: String,
    val revision: Long,
    val payload: String
)

@Entity(tableName = "shared_operations")
data class DbSharedOperation(
    @PrimaryKey val id: String,
    val listId: String,
    val participantId: String,
    val payload: String
)

@Entity(tableName = "shared_activity", primaryKeys = ["listId", "eventId"])
data class DbSharedActivity(val listId: String, val eventId: String, val sequence: Long, val payload: String)

@Entity(tableName = "shared_activity_cursors")
data class DbSharedActivityCursor(@PrimaryKey val listId: String, val nextBefore: Long?)

@Dao
interface SharedCacheDao {
    @Query("SELECT * FROM shared_lists")
    fun observeLists(): Flow<List<DbSharedList>>

    @Query("SELECT * FROM shared_lists")
    suspend fun lists(): List<DbSharedList>

    @Query("SELECT * FROM shared_lists WHERE listId = :id")
    suspend fun get(id: String): DbSharedList?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(list: DbSharedList)

    @Query("DELETE FROM shared_lists WHERE listId = :id")
    suspend fun remove(id: String)

}

package com.jcotters.database.profile

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceProfileDao {
    @Query("SELECT * FROM device_profile WHERE id = 1")
    fun observe(): Flow<DeviceProfile?>

    @Query("SELECT * FROM device_profile WHERE id = 1")
    suspend fun get(): DeviceProfile?

    @Query("UPDATE device_profile SET displayName = :name, nameConfirmed = 1 WHERE id = 1")
    suspend fun saveName(name: String)

    @Query("UPDATE device_profile SET color = :color WHERE id = 1")
    suspend fun saveColor(color: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(profile: DeviceProfile)
}

package com.jcotters.database.profile

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_profile")
data class DeviceProfile(
    @PrimaryKey val id: Int = 1,
    val installationId: String,
    val displayName: String,
    @androidx.room.ColumnInfo(defaultValue = "0") val color: Int = 0,
    @androidx.room.ColumnInfo(defaultValue = "0") val nameConfirmed: Boolean = false,
)

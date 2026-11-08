package com.jcotters.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun databaseBuilder(): RoomDatabase.Builder<MoversDatabase> {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        NSApplicationSupportDirectory, NSUserDomainMask, null, true, null,
    ) ?: error("Application Support directory is unavailable")
    return Room.databaseBuilder<MoversDatabase>(
        name = requireNotNull(directory.path) + "/movers_database",
    )
}

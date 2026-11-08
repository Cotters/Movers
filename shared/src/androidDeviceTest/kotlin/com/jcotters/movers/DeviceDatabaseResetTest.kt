package com.jcotters.movers

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jcotters.database.MoversDatabase
import com.jcotters.database.createDatabase
import com.jcotters.database.profile.DeviceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DeviceDatabaseResetTest {
    @Test fun versionSixIsResetOnceAndNewDeviceDataSurvivesReopening() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "reset-test-${UUID.randomUUID()}"
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        try {
            SQLiteDatabase.openOrCreateDatabase(path, null).use { legacy ->
                legacy.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY, username TEXT)")
                legacy.execSQL("INSERT INTO users VALUES (1, 'legacy')")
                legacy.version = 6
            }
            val database = createDatabase(Room.databaseBuilder<MoversDatabase>(context, name), Dispatchers.IO)
            try {
                assertNull(database.profileDao().get())
                assertTrue(database.movieDao().getAllMovies().isEmpty())
                database.profileDao().insert(DeviceProfile(installationId = "device-test", displayName = "Josh"))
            } finally { database.close() }
            SQLiteDatabase.openOrCreateDatabase(path, null).use { connection ->
                connection.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='users'", null).use {
                    assertFalse(it.moveToFirst())
                }
            }
            val reopened = createDatabase(Room.databaseBuilder<MoversDatabase>(context, name), Dispatchers.IO)
            try {
                assertEquals("device-test", reopened.profileDao().get()?.installationId)
                assertEquals("Josh", reopened.profileDao().get()?.displayName)
            } finally { reopened.close() }
        } finally { context.deleteDatabase(name) }
    }
}

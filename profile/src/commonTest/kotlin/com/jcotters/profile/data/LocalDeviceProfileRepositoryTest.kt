package com.jcotters.profile.data

import com.jcotters.database.profile.DeviceProfile
import com.jcotters.database.profile.DeviceProfileDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocalDeviceProfileRepositoryTest {
    @Test
    fun newInstallationRequiresANameAndKeepsItsIdentity() = runTest {
        val dao = MemoryProfileDao()
        val repository = LocalDeviceProfileRepository(dao)
        repository.initialize()
        val first = assertNotNull(repository.profile.first())
        assertEquals("", first.displayName)
        assertEquals(false, first.nameConfirmed)
        repository.saveName("Josh")
        val confirmed = assertNotNull(repository.profile.first())
        assertEquals("Josh", confirmed.displayName)
        assertTrue(confirmed.nameConfirmed)
        assertTrue(first.installationId.isNotBlank())
        val color = (confirmed.color + 1) % 8
        repository.saveColor(color)
        LocalDeviceProfileRepository(dao).initialize()
        assertEquals(confirmed.copy(color = color), repository.profile.first())
        assertEquals(1, dao.insertions)
    }

    @Test
    fun storedProfileIsNotReplacedOnStartup() = runTest {
        val dao = MemoryProfileDao(DeviceProfile(installationId = "existing", displayName = "Josh"))
        val repository = LocalDeviceProfileRepository(dao)
        repository.initialize()
        assertEquals("existing", repository.profile.first()?.installationId)
        assertEquals(0, dao.insertions)
    }

    private class MemoryProfileDao(initial: DeviceProfile? = null) : DeviceProfileDao {
        private val row = MutableStateFlow(initial)
        var insertions = 0
        override fun observe() = row
        override suspend fun get() = row.value
        override suspend fun saveName(name: String) {
            row.value = row.value?.copy(displayName = name, nameConfirmed = true)
        }

        override suspend fun saveColor(color: Int) {
            row.value = row.value?.copy(color = color)
        }

        override suspend fun insert(profile: DeviceProfile) {
            insertions++
            if (row.value == null) row.value = profile
        }
    }
}

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
    @Test fun newInstallationStartsAsJoshAndKeepsItsIdentity() = runTest {
        val dao = MemoryProfileDao()
        val repository = LocalDeviceProfileRepository(dao)
        repository.initialize()
        val first = assertNotNull(repository.profile.first())
        assertEquals("Josh", first.displayName)
        assertTrue(first.installationId.isNotBlank())
        LocalDeviceProfileRepository(dao).initialize()
        assertEquals(first, repository.profile.first())
        assertEquals(1, dao.insertions)
    }
    @Test fun storedProfileIsNotReplacedOnStartup() = runTest {
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
        override suspend fun insert(profile: DeviceProfile) {
            insertions++
            if (row.value == null) row.value = profile
        }
    }
}

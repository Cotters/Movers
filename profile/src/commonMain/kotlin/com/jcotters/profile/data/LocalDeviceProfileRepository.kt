package com.jcotters.profile.data

import com.jcotters.database.profile.DeviceProfile
import com.jcotters.database.profile.DeviceProfileDao
import com.jcotters.profile.domain.DeviceProfileRepository
import com.jcotters.profile.domain.Profile
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

internal class LocalDeviceProfileRepository(private val dao: DeviceProfileDao) : DeviceProfileRepository {
    override val profile = dao.observe().map { it?.let { Profile(it.installationId, it.displayName) } }

    override suspend fun initialize() {
        if (dao.get() == null) {
            dao.insert(DeviceProfile(installationId = Uuid.random().toString(), displayName = "Josh"))
        }
    }
}

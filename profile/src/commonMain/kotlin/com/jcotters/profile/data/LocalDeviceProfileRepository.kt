package com.jcotters.profile.data

import com.jcotters.database.profile.DeviceProfile
import com.jcotters.database.profile.DeviceProfileDao
import com.jcotters.profile.domain.DeviceProfileRepository
import com.jcotters.profile.domain.Profile
import com.jcotters.profile.domain.ParticipantIdentity
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.Uuid

internal class LocalDeviceProfileRepository(
    private val dao: DeviceProfileDao,
    private val identity: ParticipantIdentity? = null,
) : DeviceProfileRepository {
    private val edits = Mutex()
    override val profile = dao.observe().map { it?.let { Profile(it.installationId, it.displayName, it.color, it.nameConfirmed) } }

    override suspend fun saveName(name: String): Unit = edits.withLock {
        val valid = name.trim()
        require(valid.isNotEmpty() && valid.length <= 80) { "Enter a name of 1–80 characters." }
        val existing = checkNotNull(dao.get())
        if (identity?.configured == true && identity.currentId() != null) identity.update(valid, existing.color)
        dao.saveName(valid)
    }

    override suspend fun saveColor(color: Int): Unit = edits.withLock {
        require(color in 0..7) { "Choose an avatar colour." }
        val existing = checkNotNull(dao.get())
        if (existing.color != color) {
            if (identity?.configured == true && identity.currentId() != null) {
                identity.update(existing.displayName, color)
            }
            dao.saveColor(color)
        }
    }

    override suspend fun initialize() {
        if (dao.get() == null) {
            dao.insert(DeviceProfile(installationId = Uuid.random().toString(), displayName = "", color = kotlin.random.Random.nextInt(8)))
        }
    }
}

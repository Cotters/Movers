package com.jcotters.profile

import com.jcotters.database.MoversDatabase
import com.jcotters.profile.data.LocalDeviceProfileRepository
import com.jcotters.profile.domain.DeviceProfileRepository
import com.jcotters.profile.ui.ProfileViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileDependencies = module {
    single<DeviceProfileRepository> { LocalDeviceProfileRepository(get<MoversDatabase>().profileDao(), getOrNull()) }
    viewModelOf(::ProfileViewModel)
}

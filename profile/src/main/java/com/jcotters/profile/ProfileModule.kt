package com.jcotters.profile

import com.jcotters.profile.data.ProfileRepository
import com.jcotters.profile.domain.IProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent

@Module
@InstallIn(ActivityRetainedComponent::class)
abstract class ProfileModule {

  @Binds
  abstract fun bindProfileRepository(
    profileRepository: ProfileRepository
  ): IProfileRepository

}
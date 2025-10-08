package com.jcotters.auth

import com.jcotters.auth.data.UserRepository
import com.jcotters.auth.domain.IUserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
object AuthModule {
    @Provides
    fun provideUserRepository(impl: UserRepository): IUserRepository = impl
}
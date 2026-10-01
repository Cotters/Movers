package com.jcotters.auth

import com.jcotters.auth.data.UserRepository
import com.jcotters.auth.domain.IUserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AuthModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepository): IUserRepository

    companion object {
        @Provides
        fun provideSessionClock(): Clock = Clock.systemUTC()
    }
}

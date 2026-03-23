package com.bowlof.lightchecker.di

import com.bowlof.lightchecker.data.repository.LocationsRepositoryImpl
import com.bowlof.lightchecker.data.repository.ScheduleRepositoryImpl
import com.bowlof.lightchecker.domain.repository.LocationsRepository
import com.bowlof.lightchecker.domain.repository.ScheduleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository

    @Binds
    @Singleton
    abstract fun bindLocationsRepository(impl: LocationsRepositoryImpl): LocationsRepository
}

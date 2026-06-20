package com.bowlof.lightchecker.di

import com.bowlof.lightchecker.data.catalog.CitiesCatalogLoader
import com.bowlof.lightchecker.data.debug.DemoDataSeederImpl
import com.bowlof.lightchecker.data.location.DeviceLocationReader
import com.bowlof.lightchecker.data.repository.LocationsRepositoryImpl
import com.bowlof.lightchecker.data.repository.ScheduleRepositoryImpl
import com.bowlof.lightchecker.data.repository.SyncHistoryRepositoryImpl
import com.bowlof.lightchecker.domain.catalog.CityCatalogProvider
import com.bowlof.lightchecker.domain.debug.DemoDataSeeder
import com.bowlof.lightchecker.domain.location.DeviceLocationProvider
import com.bowlof.lightchecker.domain.repository.LocationsRepository
import com.bowlof.lightchecker.domain.repository.ScheduleRepository
import com.bowlof.lightchecker.domain.repository.SyncHistoryRepository
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

    @Binds
    @Singleton
    abstract fun bindSyncHistoryRepository(impl: SyncHistoryRepositoryImpl): SyncHistoryRepository

    @Binds
    @Singleton
    abstract fun bindCityCatalogProvider(impl: CitiesCatalogLoader): CityCatalogProvider

    @Binds
    @Singleton
    abstract fun bindDeviceLocationProvider(impl: DeviceLocationReader): DeviceLocationProvider

    @Binds
    @Singleton
    abstract fun bindDemoDataSeeder(impl: DemoDataSeederImpl): DemoDataSeeder
}

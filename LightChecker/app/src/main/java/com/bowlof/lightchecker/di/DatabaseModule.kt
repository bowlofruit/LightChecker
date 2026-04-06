package com.bowlof.lightchecker.di

import android.content.Context
import androidx.room.Room
import com.bowlof.lightchecker.data.local.db.LightCheckerDatabase
import com.bowlof.lightchecker.data.local.db.OutageSlotDao
import com.bowlof.lightchecker.data.local.db.SavedLocationDao
import com.bowlof.lightchecker.data.local.db.SyncEventDao
import com.bowlof.lightchecker.data.local.db.SyncHistoryDao
import com.bowlof.lightchecker.data.local.db.SyncMetaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LightCheckerDatabase {
        return Room.databaseBuilder(
            context,
            LightCheckerDatabase::class.java,
            "light_checker.db",
        ).addMigrations(LightCheckerDatabase.MIGRATION_5_6, LightCheckerDatabase.MIGRATION_6_7)
            .build()
    }

    @Provides
    fun provideSavedLocationDao(database: LightCheckerDatabase): SavedLocationDao =
        database.savedLocationDao()

    @Provides
    fun provideSyncMetaDao(database: LightCheckerDatabase): SyncMetaDao =
        database.syncMetaDao()

    @Provides
    fun provideOutageSlotDao(database: LightCheckerDatabase): OutageSlotDao =
        database.outageSlotDao()

    @Provides
    fun provideSyncHistoryDao(database: LightCheckerDatabase): SyncHistoryDao =
        database.syncHistoryDao()

    @Provides
    fun provideSyncEventDao(database: LightCheckerDatabase): SyncEventDao =
        database.syncEventDao()
}

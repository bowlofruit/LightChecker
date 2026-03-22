package com.bowlof.lightchecker.di

import android.content.Context
import androidx.room.Room
import com.bowlof.lightchecker.data.local.db.LightCheckerDatabase
import com.bowlof.lightchecker.data.local.db.SyncMetadataDao
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
        ).build()
    }

    @Provides
    fun provideSyncMetadataDao(database: LightCheckerDatabase): SyncMetadataDao {
        return database.syncMetadataDao()
    }
}

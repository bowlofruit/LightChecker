package com.bowlof.lightchecker.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SyncMetadataEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LightCheckerDatabase : RoomDatabase() {

    abstract fun syncMetadataDao(): SyncMetadataDao
}

package com.bowlof.lightchecker.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SavedLocationEntity::class,
        SyncMetaEntity::class,
        OutageSlotEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class LightCheckerDatabase : RoomDatabase() {

    abstract fun savedLocationDao(): SavedLocationDao

    abstract fun syncMetaDao(): SyncMetaDao

    abstract fun outageSlotDao(): OutageSlotDao
}

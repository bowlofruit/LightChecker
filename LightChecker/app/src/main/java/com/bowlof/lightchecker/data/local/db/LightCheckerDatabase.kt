package com.bowlof.lightchecker.data.local.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SavedLocationEntity::class,
        SyncMetaEntity::class,
        OutageSlotEntity::class,
        SyncHistoryEntity::class,
        SyncEventEntity::class,
    ],
    version = 7,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 4, to = 5),
    ],
)
abstract class LightCheckerDatabase : RoomDatabase() {

    abstract fun savedLocationDao(): SavedLocationDao

    abstract fun syncMetaDao(): SyncMetaDao

    abstract fun outageSlotDao(): OutageSlotDao

    abstract fun syncHistoryDao(): SyncHistoryDao

    abstract fun syncEventDao(): SyncEventDao

    companion object {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestamp_millis` INTEGER NOT NULL,
                        `event_type` TEXT NOT NULL,
                        `region_id` TEXT NOT NULL,
                        `queue_id` TEXT NOT NULL,
                        `details` TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `region_id` TEXT NOT NULL,
                        `queue_id` TEXT NOT NULL,
                        `date_yyyymmdd` INTEGER NOT NULL,
                        `old_version` INTEGER,
                        `new_version` INTEGER NOT NULL,
                        `synced_at_epoch_millis` INTEGER NOT NULL,
                        `city_display_name` TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}

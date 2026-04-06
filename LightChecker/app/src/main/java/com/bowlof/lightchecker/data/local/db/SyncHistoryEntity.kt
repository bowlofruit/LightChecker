package com.bowlof.lightchecker.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_history")
data class SyncHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "region_id") val regionId: String,
    @ColumnInfo(name = "queue_id") val queueId: String,
    @ColumnInfo(name = "date_yyyymmdd") val dateYyyymmdd: Long,
    @ColumnInfo(name = "old_version") val oldVersion: Long?,
    @ColumnInfo(name = "new_version") val newVersion: Long,
    @ColumnInfo(name = "synced_at_epoch_millis") val syncedAtEpochMillis: Long,
    @ColumnInfo(name = "city_display_name") val cityDisplayName: String = "",
)

package com.bowlof.lightchecker.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "sync_meta",
    primaryKeys = ["region_id", "queue_id", "effective_date_yyyymmdd"],
)
data class SyncMetaEntity(
    @ColumnInfo(name = "region_id") val regionId: String,
    @ColumnInfo(name = "queue_id") val queueId: String,
    @ColumnInfo(name = "effective_date_yyyymmdd") val effectiveDateYyyymmdd: Long,
    @ColumnInfo(name = "cached_version") val cachedVersion: Long,
    @ColumnInfo(name = "last_sync_success_at") val lastSyncSuccessAtEpochMillis: Long?,
)

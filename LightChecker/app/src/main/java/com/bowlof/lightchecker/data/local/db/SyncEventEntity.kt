package com.bowlof.lightchecker.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_events")
data class SyncEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "timestamp_millis") val timestampMillis: Long,
    @ColumnInfo(name = "event_type") val eventType: String,
    @ColumnInfo(name = "region_id") val regionId: String,
    @ColumnInfo(name = "queue_id") val queueId: String,
    @ColumnInfo(name = "details") val details: String = "",
)

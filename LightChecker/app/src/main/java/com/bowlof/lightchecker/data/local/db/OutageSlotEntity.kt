package com.bowlof.lightchecker.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "outage_slots",
    primaryKeys = ["region_id", "queue_id", "effective_date_yyyymmdd", "slot_index"],
    indices = [
        Index(value = ["region_id", "queue_id", "effective_date_yyyymmdd"]),
        Index(value = ["effective_date_yyyymmdd"]),
    ],
)
data class OutageSlotEntity(
    @ColumnInfo(name = "region_id") val regionId: String,
    @ColumnInfo(name = "queue_id") val queueId: String,
    @ColumnInfo(name = "effective_date_yyyymmdd") val effectiveDateYyyymmdd: Long,
    @ColumnInfo(name = "slot_index") val slotIndex: Int,
    @ColumnInfo(name = "start_minute") val startMinute: Int,
    @ColumnInfo(name = "end_minute") val endMinute: Int,
)

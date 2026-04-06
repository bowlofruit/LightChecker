package com.bowlof.lightchecker.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "region_id") val regionId: String,
    @ColumnInfo(name = "queue_id") val queueId: String,
    @ColumnInfo(name = "city_id") val cityId: String,
    @ColumnInfo(name = "city_display_name") val cityDisplayName: String,
    @ColumnInfo(name = "queue_display_name") val queueDisplayName: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "is_widget_primary") val isWidgetPrimary: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    @ColumnInfo(name = "location_source") val locationSource: String?,
    @ColumnInfo(name = "notifications_enabled", defaultValue = "1") val notificationsEnabled: Boolean = true,
)

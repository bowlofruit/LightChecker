package com.bowlof.lightchecker.domain.model

data class SavedPlace(
    val id: Long,
    val regionId: String,
    val queueId: String,
    val cityId: String,
    val cityDisplayName: String,
    val queueDisplayName: String,
    val sortOrder: Int,
    val isWidgetPrimary: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val locationSource: LocationSource?,
    val notificationsEnabled: Boolean = true,
)

package com.bowlof.lightchecker.domain.repository

import com.bowlof.lightchecker.domain.model.SavedPlace
import kotlinx.coroutines.flow.Flow

interface LocationsRepository {

    fun observeSavedPlaces(): Flow<List<SavedPlace>>

    fun observeSavedPlaceCount(): Flow<Int>

    fun observePrimaryPlace(): Flow<SavedPlace?>

    suspend fun addPlace(
        regionId: String,
        queueId: String,
        cityId: String,
        cityDisplayName: String,
        queueDisplayName: String,
        latitude: Double?,
        longitude: Double?,
        locationSource: com.bowlof.lightchecker.domain.model.LocationSource?,
        setAsWidgetPrimary: Boolean,
    ): Long

    suspend fun deletePlace(id: Long)

    suspend fun setWidgetPrimary(id: Long)

    suspend fun setNotificationsEnabled(id: Long, enabled: Boolean)

    suspend fun swapSortOrder(idA: Long, idB: Long)
}

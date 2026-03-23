package com.bowlof.lightchecker.data.local

import com.bowlof.lightchecker.data.local.db.SavedLocationEntity
import com.bowlof.lightchecker.domain.model.LocationSource
import com.bowlof.lightchecker.domain.model.SavedPlace

internal fun SavedLocationEntity.toDomain(): SavedPlace {
    return SavedPlace(
        id = id,
        regionId = regionId,
        queueId = queueId,
        cityId = cityId,
        cityDisplayName = cityDisplayName,
        queueDisplayName = queueDisplayName,
        sortOrder = sortOrder,
        isWidgetPrimary = isWidgetPrimary,
        latitude = latitude,
        longitude = longitude,
        locationSource = locationSource?.let { runCatching { LocationSource.valueOf(it) }.getOrNull() },
    )
}

package com.bowlof.lightchecker.domain.location

import com.bowlof.lightchecker.domain.model.DeviceLocation

/** Постачальник грубої геопозиції пристрою (абстракція над Fused/Geocoder). */
interface DeviceLocationProvider {

    suspend fun getLastLocationOrNull(): DeviceLocation?
}

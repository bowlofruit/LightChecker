package com.bowlof.lightchecker.domain.model

/** Доменна модель грубої геопозиції пристрою з опційною назвою населеного пункту. */
data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val locality: String?,
)

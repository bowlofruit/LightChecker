package com.bowlof.lightchecker.domain.model

/**
 * Канонічна доменна модель довідника міст/черг (єдина для presentation і domain).
 */
data class CityCatalog(
    val catalogVersion: Int,
    val cities: List<CatalogCity>,
)

data class CatalogCity(
    val cityId: String,
    val displayName: String,
    val queues: List<CatalogQueue>,
)

data class CatalogQueue(
    val queueId: String,
    val displayName: String,
    val regionId: String,
)

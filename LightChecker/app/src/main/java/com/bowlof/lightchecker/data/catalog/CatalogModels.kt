package com.bowlof.lightchecker.data.catalog

data class CitiesCatalog(
    val catalogVersion: Int,
    val cities: List<CityCatalogEntry>,
)

data class CityCatalogEntry(
    val cityId: String,
    val displayName: String,
    val queues: List<QueueCatalogEntry>,
)

data class QueueCatalogEntry(
    val queueId: String,
    val displayName: String,
    val regionId: String,
)

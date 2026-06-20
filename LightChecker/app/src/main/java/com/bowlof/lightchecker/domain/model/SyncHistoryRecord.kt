package com.bowlof.lightchecker.domain.model

/**
 * Доменна модель запису історії синхронізації (без прив'язки до Room).
 */
data class SyncHistoryRecord(
    val id: Long,
    val regionId: String,
    val queueId: String,
    val dateYyyymmdd: Long,
    val oldVersion: Long?,
    val newVersion: Long,
    val syncedAtEpochMillis: Long,
    val cityDisplayName: String,
)

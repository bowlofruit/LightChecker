package com.bowlof.lightchecker.domain.model

/** Графік на один календарний день (Kyiv `effectiveDateYyyymmdd`) з відомою кешованою версією з бекенду, якщо був успішний sync. */
data class DaySchedule(
    val effectiveDateYyyymmdd: Long,
    val cachedVersion: Long?,
    val intervals: List<OutageInterval>,
    val lastSyncAtEpochMillis: Long? = null,
)

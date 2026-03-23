package com.bowlof.lightchecker.domain.repository

import com.bowlof.lightchecker.domain.model.DaySchedule
import com.bowlof.lightchecker.domain.model.OutageInterval
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {

    fun observeIntervals(
        regionId: String,
        queueId: String,
        effectiveDateYyyymmdd: Long,
    ): Flow<List<OutageInterval>>

    fun observeDaySchedule(
        regionId: String,
        queueId: String,
        effectiveDateYyyymmdd: Long,
    ): Flow<DaySchedule>

    suspend fun refreshSchedule(regionId: String, queueId: String)

    suspend fun syncIfNewerVersion(
        regionId: String,
        queueId: String,
        remoteVersion: Long?,
        remoteDay: Long?,
    )

    suspend fun purgeStaleCache()
}

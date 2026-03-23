@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.bowlof.lightchecker.domain.usecase

import com.bowlof.lightchecker.domain.model.OutageInterval
import com.bowlof.lightchecker.domain.repository.LocationsRepository
import com.bowlof.lightchecker.domain.repository.ScheduleRepository
import com.bowlof.lightchecker.domain.time.KyivTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.Clock
import javax.inject.Inject

data class WidgetDaySchedule(
    val effectiveDateYyyymmdd: Long,
    val labelIsTomorrow: Boolean,
    val intervals: List<OutageInterval>,
)

class GetWidgetDayScheduleUseCase @Inject constructor(
    private val locationsRepository: LocationsRepository,
    private val scheduleRepository: ScheduleRepository,
    private val clock: Clock,
) {

    fun observe(): Flow<WidgetDaySchedule?> {
        return locationsRepository.observePrimaryPlace().flatMapLatest { primary ->
            if (primary == null) return@flatMapLatest flowOf(null)
            val today = KyivTime.todayYyyymmdd(clock)
            val tomorrow = KyivTime.tomorrowYyyymmdd(clock)
            combine(
                scheduleRepository.observeIntervals(primary.regionId, primary.queueId, today),
                scheduleRepository.observeIntervals(primary.regionId, primary.queueId, tomorrow),
            ) { todaySlots, tomorrowSlots ->
                when {
                    todaySlots.isNotEmpty() -> WidgetDaySchedule(today, false, todaySlots)
                    tomorrowSlots.isNotEmpty() -> WidgetDaySchedule(tomorrow, true, tomorrowSlots)
                    else -> WidgetDaySchedule(today, false, emptyList())
                }
            }
        }
    }
}

package com.bowlof.lightchecker.domain.usecase

import com.bowlof.lightchecker.domain.model.DaySchedule
import com.bowlof.lightchecker.domain.model.SelectedScheduleDay
import com.bowlof.lightchecker.domain.repository.ScheduleRepository
import com.bowlof.lightchecker.domain.time.KyivTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Clock
import javax.inject.Inject

class GetDayScheduleForPlaceUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val clock: Clock,
) {

    fun observe(
        regionId: String,
        queueId: String,
        selectedDay: SelectedScheduleDay,
    ): Flow<DaySchedule> {
        val day = when (selectedDay) {
            SelectedScheduleDay.Today -> KyivTime.todayYyyymmdd(clock)
            SelectedScheduleDay.Tomorrow -> KyivTime.tomorrowYyyymmdd(clock)
        }
        return scheduleRepository.observeDaySchedule(regionId, queueId, day)
            .distinctUntilChanged()
    }
}

package com.bowlof.lightchecker.domain.usecase

import app.cash.turbine.test
import com.bowlof.lightchecker.domain.model.DaySchedule
import com.bowlof.lightchecker.domain.model.SelectedScheduleDay
import com.bowlof.lightchecker.domain.repository.ScheduleRepository
import com.bowlof.lightchecker.domain.time.KyivTime
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.ZonedDateTime

class GetDayScheduleForPlaceUseCaseTest {

    private val clock: Clock = Clock.fixed(
        ZonedDateTime.of(2026, 6, 15, 12, 0, 0, 0, KyivTime.zone).toInstant(),
        KyivTime.zone,
    )

    @Test
    fun `observe today uses today yyyymmdd`() = runTest {
        val today = KyivTime.todayYyyymmdd(clock)
        val scheduleRepo = mockk<ScheduleRepository>()
        every { scheduleRepo.observeDaySchedule("reg", "q", today) } returns flowOf(
            DaySchedule(today, 1L, emptyList()),
        )
        val useCase = GetDayScheduleForPlaceUseCase(scheduleRepo, clock)
        useCase.observe("reg", "q", SelectedScheduleDay.Today).test {
            assertEquals(today, awaitItem().effectiveDateYyyymmdd)
            awaitComplete()
        }
    }

    @Test
    fun `observe tomorrow uses tomorrow yyyymmdd`() = runTest {
        val tomorrow = KyivTime.tomorrowYyyymmdd(clock)
        val scheduleRepo = mockk<ScheduleRepository>()
        every { scheduleRepo.observeDaySchedule("reg", "q", tomorrow) } returns flowOf(
            DaySchedule(tomorrow, 2L, emptyList()),
        )
        val useCase = GetDayScheduleForPlaceUseCase(scheduleRepo, clock)
        useCase.observe("reg", "q", SelectedScheduleDay.Tomorrow).test {
            assertEquals(tomorrow, awaitItem().effectiveDateYyyymmdd)
            awaitComplete()
        }
    }
}

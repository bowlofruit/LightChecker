@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.bowlof.lightchecker.presentation.schedule

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.bowlof.lightchecker.domain.model.DaySchedule
import com.bowlof.lightchecker.domain.model.LocationSource
import com.bowlof.lightchecker.domain.model.OutageInterval
import com.bowlof.lightchecker.domain.model.SavedPlace
import com.bowlof.lightchecker.domain.repository.LocationsRepository
import com.bowlof.lightchecker.domain.repository.ScheduleRepository
import com.bowlof.lightchecker.domain.repository.UiPreferencesRepository
import com.bowlof.lightchecker.domain.time.KyivTime
import com.bowlof.lightchecker.domain.usecase.GetDayScheduleForPlaceUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.Clock
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScheduleViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scheduleRepo = mockk<ScheduleRepository>()
    private val locationsRepo = mockk<LocationsRepository>()
    private val clock: Clock = Clock.fixed(
        ZonedDateTime.of(2026, 6, 15, 12, 0, 0, 0, KyivTime.zone).toInstant(),
        KyivTime.zone,
    )
    private val useCase = GetDayScheduleForPlaceUseCase(scheduleRepo, clock)

    private val uiPreferences = mockk<UiPreferencesRepository>(relaxed = true)

    private val place = SavedPlace(
        id = 1L,
        regionId = "reg",
        queueId = "q",
        cityId = "kyiv",
        cityDisplayName = "Київ",
        queueDisplayName = "2.1",
        sortOrder = 0,
        isWidgetPrimary = true,
        latitude = null,
        longitude = null,
        locationSource = LocationSource.USER_MANUAL,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        every { uiPreferences.demoUiScheduleEnabled } returns flowOf(false)
        every { locationsRepo.observeSavedPlaces() } returns flowOf(listOf(place))
        val today = KyivTime.todayYyyymmdd(clock)
        every { scheduleRepo.observeDaySchedule("reg", "q", today) } returns flowOf(
            DaySchedule(
                effectiveDateYyyymmdd = today,
                cachedVersion = 1L,
                intervals = listOf(OutageInterval(60, 120)),
            ),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState exposes formatted intervals for today`() = runTest(mainDispatcher) {
        val vm = ScheduleViewModel(context, locationsRepo, scheduleRepo, uiPreferences, useCase)
        backgroundScope.launch(mainDispatcher) { vm.uiState.collect { } }
        advanceUntilIdle()
        assertTrue(vm.uiState.value.intervalLines.isNotEmpty())
    }

    @Test
    fun `refresh failure emits snackbar message`() = runTest(mainDispatcher) {
        coEvery { scheduleRepo.refreshSchedule(any(), any()) } throws IOException("net")
        val vm = ScheduleViewModel(context, locationsRepo, scheduleRepo, uiPreferences, useCase)
        backgroundScope.launch(mainDispatcher) { vm.uiState.collect { } }
        advanceUntilIdle()
        vm.snackbarMessages.test {
            vm.refresh()
            advanceUntilIdle()
            assertTrue(awaitItem().isNotBlank())
        }
    }
}

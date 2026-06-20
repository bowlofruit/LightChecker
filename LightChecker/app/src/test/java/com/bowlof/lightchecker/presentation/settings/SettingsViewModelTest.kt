@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.bowlof.lightchecker.presentation.settings

import app.cash.turbine.test
import com.bowlof.lightchecker.domain.model.SavedPlace
import com.bowlof.lightchecker.domain.repository.LocationsRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repo: LocationsRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        every { repo.observeSavedPlaces() } returns flowOf(listOf(testPlace()))
        viewModel = SettingsViewModel(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `places flow emits saved places from repository`() = runTest(testDispatcher) {
        viewModel.places.test {
            assertEquals(emptyList<SavedPlace>(), awaitItem())
            assertEquals(listOf(testPlace()), awaitItem())
        }
    }

    @Test
    fun `deletePlace calls repository deletePlace`() = runTest(testDispatcher) {
        viewModel.deletePlace(1L)
        advanceUntilIdle()
        coVerify { repo.deletePlace(1L) }
    }

    @Test
    fun `setWidgetPrimary calls repository setWidgetPrimary`() = runTest(testDispatcher) {
        viewModel.setWidgetPrimary(1L)
        advanceUntilIdle()
        coVerify { repo.setWidgetPrimary(1L) }
    }

    @Test
    fun `toggleNotifications calls repository setNotificationsEnabled`() = runTest(testDispatcher) {
        viewModel.toggleNotifications(1L, false)
        advanceUntilIdle()
        coVerify { repo.setNotificationsEnabled(1L, false) }
    }

    @Test
    fun `swapOrder calls repository swapSortOrder`() = runTest(testDispatcher) {
        viewModel.swapOrder(1L, 2L)
        advanceUntilIdle()
        coVerify { repo.swapSortOrder(1L, 2L) }
    }

    private fun testPlace(id: Long = 1) = SavedPlace(
        id = id,
        regionId = "r",
        queueId = "q",
        cityId = "c",
        cityDisplayName = "City",
        queueDisplayName = "Queue 1",
        sortOrder = 0,
        isWidgetPrimary = false,
        latitude = null,
        longitude = null,
        locationSource = null,
    )
}

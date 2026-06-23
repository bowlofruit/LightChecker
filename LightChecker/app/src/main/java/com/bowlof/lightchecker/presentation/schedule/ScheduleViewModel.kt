@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.bowlof.lightchecker.presentation.schedule

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlof.lightchecker.BuildConfig
import com.bowlof.lightchecker.domain.model.OutageInterval
import com.bowlof.lightchecker.domain.model.SavedPlace
import com.bowlof.lightchecker.domain.model.SelectedScheduleDay
import com.bowlof.lightchecker.domain.repository.LocationsRepository
import com.bowlof.lightchecker.domain.repository.ScheduleRepository
import com.bowlof.lightchecker.domain.usecase.GetDayScheduleForPlaceUseCase
import com.bowlof.lightchecker.presentation.util.DemoSchedulePreview
import com.bowlof.lightchecker.presentation.util.OutageIntervalFormatter
import com.bowlof.lightchecker.presentation.util.toScheduleUserMessage
import com.bowlof.lightchecker.widget.OutageGlanceAppWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class ScheduleUiState(
    val places: List<SavedPlace> = emptyList(),
    val selectedTabIndex: Int = 0,
    val selectedDay: SelectedScheduleDay = SelectedScheduleDay.Today,
    val intervalLines: List<String> = emptyList(),
    val isRefreshing: Boolean = false,
    val hasDataForSelectedDay: Boolean = false,
    val lastSyncFormatted: String? = null,
    val intervals: List<OutageInterval> = emptyList(),
    val isDemoUiData: Boolean = false,
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val locationsRepository: LocationsRepository,
    private val scheduleRepository: ScheduleRepository,
    private val preferences: DataStore<Preferences>,
    private val getDayScheduleForPlaceUseCase: GetDayScheduleForPlaceUseCase,
) : ViewModel() {

    private val _selectedTabIndex = MutableStateFlow(0)
    private val _selectedDay = MutableStateFlow(SelectedScheduleDay.Today)
    private val _isRefreshing = MutableStateFlow(false)

    private val demoUiFromPrefs = preferences.data
        .map { it[DemoSchedulePreview.uiDemoScheduleKey] == true }
        .distinctUntilChanged()

    init {
        if (autoRefreshTriggered.compareAndSet(false, true)) {
            viewModelScope.launch {
                locationsRepository.observeSavedPlaces().first().forEach { place ->
                    runCatching { scheduleRepository.refreshSchedule(place.regionId, place.queueId) }
                }
            }
        }
    }

    val selectedDay = _selectedDay.asStateFlow()

    private val snackbarChannel = Channel<String>(Channel.BUFFERED)
    val snackbarMessages = snackbarChannel.receiveAsFlow()

    private val placesFlow = locationsRepository.observeSavedPlaces()

    val uiState = combine(
        placesFlow,
        _selectedTabIndex,
        _selectedDay,
        _isRefreshing,
        demoUiFromPrefs,
    ) { p: List<SavedPlace>, tab: Int, day: SelectedScheduleDay, refreshing: Boolean, demoRaw: Boolean ->
        ScheduleCombineInputs(p, tab, day, refreshing, demoRaw)
    }.flatMapLatest { inputs ->
        val (p, tab, day, refreshing, demoRaw) = inputs
        val demo = BuildConfig.DEBUG && demoRaw && p.isNotEmpty()
        if (p.isEmpty()) {
            flowOf(
                ScheduleUiState(
                    places = p,
                    selectedTabIndex = tab,
                    selectedDay = day,
                    intervalLines = emptyList(),
                    isRefreshing = refreshing,
                    hasDataForSelectedDay = false,
                    isDemoUiData = false,
                ),
            )
        } else if (demo) {
            val safeTab = tab.coerceIn(0, p.lastIndex)
            flowOf(demoScheduleUiState(p, safeTab, day, refreshing))
        } else {
            val safeTab = tab.coerceIn(0, p.lastIndex)
            val place = p[safeTab]
            getDayScheduleForPlaceUseCase.observe(place.regionId, place.queueId, day)
                .map { schedule ->
                    ScheduleUiState(
                        places = p,
                        selectedTabIndex = safeTab,
                        selectedDay = day,
                        intervalLines = schedule.intervals.map { OutageIntervalFormatter.format(it) },
                        isRefreshing = refreshing,
                        hasDataForSelectedDay = schedule.intervals.isNotEmpty(),
                        lastSyncFormatted = schedule.lastSyncAtEpochMillis?.let { formatSyncTime(it) },
                        intervals = schedule.intervals,
                        isDemoUiData = false,
                    )
                }
        }
    }.distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ScheduleUiState(),
        )

    private fun formatSyncTime(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        return SYNC_TIME_FMT.format(instant.atZone(KYIV_ZONE))
    }

    companion object {
        private val autoRefreshTriggered = AtomicBoolean(false)

        private val KYIV_ZONE = ZoneId.of("Europe/Kyiv")
        private val SYNC_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")

        private fun demoScheduleUiState(
            places: List<SavedPlace>,
            selectedTabIndex: Int,
            selectedDay: SelectedScheduleDay,
            isRefreshing: Boolean,
        ): ScheduleUiState {
            val intervals = DemoSchedulePreview.intervals
            val lines = intervals.map { OutageIntervalFormatter.format(it) }
            return ScheduleUiState(
                places = places,
                selectedTabIndex = selectedTabIndex,
                selectedDay = selectedDay,
                intervalLines = lines,
                isRefreshing = isRefreshing,
                hasDataForSelectedDay = true,
                lastSyncFormatted = "12:00",
                intervals = intervals,
                isDemoUiData = true,
            )
        }
    }

    private data class ScheduleCombineInputs(
        val places: List<SavedPlace>,
        val tab: Int,
        val day: SelectedScheduleDay,
        val refreshing: Boolean,
        val demo: Boolean,
    )

    fun selectTab(index: Int) {
        _selectedTabIndex.update { index }
    }

    /** Auto-select the tab matching [regionId] and [queueId] (used for deep linking from notifications). */
    fun selectPlaceByRegionQueue(regionId: String, queueId: String) {
        val places = uiState.value.places
        val index = places.indexOfFirst { it.regionId == regionId && it.queueId == queueId }
        if (index >= 0) _selectedTabIndex.update { index }
    }

    fun setWidgetPrimary(id: Long) {
        viewModelScope.launch {
            locationsRepository.setWidgetPrimary(id)
        }
    }

    fun selectDay(day: SelectedScheduleDay) {
        _selectedDay.value = day
    }

    fun setDemoUiDataEnabled(enabled: Boolean) {
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch {
            preferences.edit { it[DemoSchedulePreview.uiDemoScheduleKey] = enabled }
            runCatching { OutageGlanceAppWidget().updateAll(appContext) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val p = uiState.value.places
            if (p.isEmpty()) return@launch
            val tab = uiState.value.selectedTabIndex.coerceIn(0, p.lastIndex)
            val place = p[tab]
            _isRefreshing.value = true
            runCatching { scheduleRepository.refreshSchedule(place.regionId, place.queueId) }
                .onFailure { e ->
                    snackbarChannel.trySend(e.toScheduleUserMessage(appContext))
                }
            _isRefreshing.value = false
        }
    }
}

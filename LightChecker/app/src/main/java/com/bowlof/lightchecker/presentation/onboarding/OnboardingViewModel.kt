package com.bowlof.lightchecker.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlof.lightchecker.domain.catalog.CityCatalogProvider
import com.bowlof.lightchecker.domain.location.DeviceLocationProvider
import com.bowlof.lightchecker.domain.model.CatalogCity
import com.bowlof.lightchecker.domain.model.LocationSource
import com.bowlof.lightchecker.domain.repository.LocationsRepository
import com.bowlof.lightchecker.domain.usecase.ResolveCityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val catalog: List<CatalogCity> = emptyList(),
    val catalogError: Boolean = false,
    val selectedCityIndex: Int = 0,
    val selectedQueueIndex: Int = 0,
    val isSaving: Boolean = false,
    val locationHint: String? = null,
    val duplicateMessage: String? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val catalogProvider: CityCatalogProvider,
    private val locationsRepository: LocationsRepository,
    private val deviceLocationProvider: DeviceLocationProvider,
    private val resolveCityUseCase: ResolveCityUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            catalogProvider.load()
                .onSuccess { cat ->
                    _ui.update {
                        it.copy(
                            catalog = cat.cities,
                            catalogError = false,
                            selectedCityIndex = 0,
                            selectedQueueIndex = 0,
                        )
                    }
                }
                .onFailure {
                    _ui.update { it.copy(catalogError = true) }
                }
        }
    }

    fun selectCity(index: Int) {
        _ui.update { it.copy(selectedCityIndex = index, selectedQueueIndex = 0) }
    }

    fun selectQueue(index: Int) {
        _ui.update { it.copy(selectedQueueIndex = index) }
    }

    fun tryResolveCityByDeviceLocation() {
        viewModelScope.launch {
            val resolved = deviceLocationProvider.getLastLocationOrNull() ?: run {
                _ui.update { it.copy(locationHint = null) }
                return@launch
            }
            val locality = resolved.locality?.lowercase() ?: run {
                _ui.update { it.copy(locationHint = null) }
                return@launch
            }
            val cities = _ui.value.catalog
            val idx = resolveCityUseCase.resolveIndex(cities, locality)
            if (idx != null) {
                _ui.update {
                    it.copy(selectedCityIndex = idx, selectedQueueIndex = 0, locationHint = cities[idx].displayName)
                }
            }
        }
    }

    fun consumeLocationHint() {
        _ui.update { it.copy(locationHint = null) }
    }

    fun saveFirstPlace(onDone: () -> Unit) {
        val state = _ui.value
        val city = state.catalog.getOrNull(state.selectedCityIndex) ?: return
        val queue = city.queues.getOrNull(state.selectedQueueIndex) ?: return
        viewModelScope.launch {
            _ui.update { it.copy(isSaving = true, duplicateMessage = null) }

            val existing = locationsRepository.observeSavedPlaces().first()
            val isDuplicate = existing.any { it.regionId == queue.regionId && it.queueId == queue.queueId }
            if (isDuplicate) {
                _ui.update {
                    it.copy(
                        isSaving = false,
                        duplicateMessage = "${city.displayName} · ${queue.displayName}",
                    )
                }
                onDone()
                return@launch
            }

            runCatching {
                locationsRepository.addPlace(
                    regionId = queue.regionId,
                    queueId = queue.queueId,
                    cityId = city.cityId,
                    cityDisplayName = city.displayName,
                    queueDisplayName = queue.displayName,
                    latitude = null,
                    longitude = null,
                    locationSource = LocationSource.USER_MANUAL,
                    setAsWidgetPrimary = true,
                )
            }
            _ui.update { it.copy(isSaving = false) }
            onDone()
        }
    }
}

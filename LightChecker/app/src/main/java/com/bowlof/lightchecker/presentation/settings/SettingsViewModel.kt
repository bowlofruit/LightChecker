package com.bowlof.lightchecker.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlof.lightchecker.domain.model.SavedPlace
import com.bowlof.lightchecker.domain.repository.LocationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val locationsRepository: LocationsRepository,
) : ViewModel() {

    val places = locationsRepository.observeSavedPlaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setWidgetPrimary(id: Long) {
        viewModelScope.launch { locationsRepository.setWidgetPrimary(id) }
    }

    fun deletePlace(id: Long) {
        viewModelScope.launch { locationsRepository.deletePlace(id) }
    }
}

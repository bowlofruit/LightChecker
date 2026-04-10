package com.bowlof.lightchecker.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlof.lightchecker.domain.repository.LocationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BootstrapViewModel @Inject constructor(
    locationsRepository: LocationsRepository,
) : ViewModel() {

    val targetRoute = locationsRepository.observeSavedPlaceCount()
        .map { count -> if (count > 0) NavRoutes.SCHEDULE else NavRoutes.ONBOARDING }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
}

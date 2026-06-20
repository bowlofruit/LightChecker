package com.bowlof.lightchecker.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlof.lightchecker.domain.model.SyncHistoryRecord
import com.bowlof.lightchecker.domain.repository.SyncHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SyncHistoryViewModel @Inject constructor(
    syncHistoryRepository: SyncHistoryRepository,
) : ViewModel() {

    val entries = syncHistoryRepository.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<SyncHistoryRecord>())
}

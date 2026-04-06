package com.bowlof.lightchecker.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlof.lightchecker.data.local.db.SyncHistoryDao
import com.bowlof.lightchecker.data.local.db.SyncHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SyncHistoryViewModel @Inject constructor(
    syncHistoryDao: SyncHistoryDao,
) : ViewModel() {

    val entries = syncHistoryDao.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<SyncHistoryEntity>())
}

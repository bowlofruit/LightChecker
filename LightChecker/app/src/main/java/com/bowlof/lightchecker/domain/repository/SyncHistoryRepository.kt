package com.bowlof.lightchecker.domain.repository

import com.bowlof.lightchecker.domain.model.SyncHistoryRecord
import kotlinx.coroutines.flow.Flow

interface SyncHistoryRepository {

    fun observeRecent(limit: Int = 100): Flow<List<SyncHistoryRecord>>
}

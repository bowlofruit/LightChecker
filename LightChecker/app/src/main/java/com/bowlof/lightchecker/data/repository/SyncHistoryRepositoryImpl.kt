package com.bowlof.lightchecker.data.repository

import com.bowlof.lightchecker.data.local.db.SyncHistoryDao
import com.bowlof.lightchecker.data.local.toDomain
import com.bowlof.lightchecker.domain.model.SyncHistoryRecord
import com.bowlof.lightchecker.domain.repository.SyncHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SyncHistoryRepositoryImpl @Inject constructor(
    private val syncHistoryDao: SyncHistoryDao,
) : SyncHistoryRepository {

    override fun observeRecent(limit: Int): Flow<List<SyncHistoryRecord>> =
        syncHistoryDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }
}

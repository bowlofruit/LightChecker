package com.bowlof.lightchecker.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncHistoryDao {

    @Query("SELECT * FROM sync_history ORDER BY synced_at_epoch_millis DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<SyncHistoryEntity>>

    @Insert
    suspend fun insert(entity: SyncHistoryEntity)

    @Query("DELETE FROM sync_history WHERE id NOT IN (SELECT id FROM sync_history ORDER BY synced_at_epoch_millis DESC LIMIT :keep)")
    suspend fun pruneOldEntries(keep: Int = 500)
}

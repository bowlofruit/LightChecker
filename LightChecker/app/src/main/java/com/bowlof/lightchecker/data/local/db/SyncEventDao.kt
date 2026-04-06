package com.bowlof.lightchecker.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SyncEventDao {
    @Insert
    suspend fun insert(entity: SyncEventEntity)

    @Query("DELETE FROM sync_events WHERE id NOT IN (SELECT id FROM sync_events ORDER BY timestamp_millis DESC LIMIT :keep)")
    suspend fun pruneOldEntries(keep: Int = 500)
}

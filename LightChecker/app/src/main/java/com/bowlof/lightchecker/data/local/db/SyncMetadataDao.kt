package com.bowlof.lightchecker.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata WHERE id = 1")
    fun observe(): Flow<SyncMetadataEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SyncMetadataEntity)
}

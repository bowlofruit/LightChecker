package com.bowlof.lightchecker.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetaDao {

    @Query(
        "SELECT * FROM sync_meta WHERE region_id = :r AND queue_id = :q AND effective_date_yyyymmdd = :d",
    )
    fun observeMeta(r: String, q: String, d: Long): Flow<SyncMetaEntity?>

    @Query(
        "SELECT * FROM sync_meta WHERE region_id = :r AND queue_id = :q AND effective_date_yyyymmdd = :d",
    )
    suspend fun get(r: String, q: String, d: Long): SyncMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SyncMetaEntity)

    @Query(
        "DELETE FROM sync_meta WHERE region_id = :r AND queue_id = :q AND effective_date_yyyymmdd = :d",
    )
    suspend fun deleteForDay(r: String, q: String, d: Long)

    @Query("DELETE FROM sync_meta WHERE effective_date_yyyymmdd < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}

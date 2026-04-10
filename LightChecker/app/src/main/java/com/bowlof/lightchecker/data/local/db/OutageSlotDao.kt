package com.bowlof.lightchecker.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OutageSlotDao {

    @Query(
        """
        SELECT * FROM outage_slots
        WHERE region_id = :r AND queue_id = :q AND effective_date_yyyymmdd = :d
        ORDER BY slot_index ASC
        """,
    )
    fun observeSlots(r: String, q: String, d: Long): Flow<List<OutageSlotEntity>>

    @Query(
        """
        DELETE FROM outage_slots
        WHERE region_id = :r AND queue_id = :q AND effective_date_yyyymmdd = :d
        """,
    )
    suspend fun deleteForDay(r: String, q: String, d: Long)

    @Insert
    suspend fun insertAll(slots: List<OutageSlotEntity>)

    @Query(
        """
        SELECT * FROM outage_slots
        WHERE region_id = :r AND queue_id = :q AND effective_date_yyyymmdd = :d
        ORDER BY slot_index ASC
        """,
    )
    suspend fun getSlots(r: String, q: String, d: Long): List<OutageSlotEntity>

    @Query(
        """
        SELECT * FROM outage_slots
        WHERE region_id = :r AND queue_id = :q AND effective_date_yyyymmdd = :d
        ORDER BY slot_index ASC
        """,
    )
    fun getSlotsSync(r: String, q: String, d: Long): List<OutageSlotEntity>

    @Query("DELETE FROM outage_slots WHERE effective_date_yyyymmdd < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}

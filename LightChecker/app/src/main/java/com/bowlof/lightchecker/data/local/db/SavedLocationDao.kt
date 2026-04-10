package com.bowlof.lightchecker.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLocationDao {

    @Query("SELECT * FROM saved_locations ORDER BY sort_order ASC, id ASC")
    fun observeAll(): Flow<List<SavedLocationEntity>>

    @Query("SELECT * FROM saved_locations")
    suspend fun getAllSnapshot(): List<SavedLocationEntity>

    @Query("SELECT COUNT(*) FROM saved_locations")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM saved_locations WHERE id = :id")
    suspend fun getById(id: Long): SavedLocationEntity?

    @Query(
        "SELECT * FROM saved_locations WHERE is_widget_primary = 1 LIMIT 1",
    )
    fun observePrimary(): Flow<SavedLocationEntity?>

    @Insert
    suspend fun insert(entity: SavedLocationEntity): Long

    @Update
    suspend fun update(entity: SavedLocationEntity)

    @Delete
    suspend fun delete(entity: SavedLocationEntity)

    @Query("UPDATE saved_locations SET is_widget_primary = 0")
    suspend fun clearAllPrimaryFlags()

    @Query("SELECT MAX(sort_order) FROM saved_locations")
    suspend fun maxSortOrder(): Int?

    @Query("UPDATE saved_locations SET notifications_enabled = :enabled WHERE id = :id")
    suspend fun setNotificationsEnabled(id: Long, enabled: Boolean)

    @Query("SELECT * FROM saved_locations ORDER BY sort_order ASC, id ASC")
    fun getAllSnapshotSync(): List<SavedLocationEntity>

    @Query("UPDATE saved_locations SET sort_order = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)
}

package com.bowlof.lightchecker.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "last_known_remote_version") val lastKnownRemoteVersion: Long? = null,
)

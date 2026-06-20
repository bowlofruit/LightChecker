package com.bowlof.lightchecker.data.local

import com.bowlof.lightchecker.data.local.db.SyncHistoryEntity
import com.bowlof.lightchecker.domain.model.SyncHistoryRecord

internal fun SyncHistoryEntity.toDomain(): SyncHistoryRecord =
    SyncHistoryRecord(
        id = id,
        regionId = regionId,
        queueId = queueId,
        dateYyyymmdd = dateYyyymmdd,
        oldVersion = oldVersion,
        newVersion = newVersion,
        syncedAtEpochMillis = syncedAtEpochMillis,
        cityDisplayName = cityDisplayName,
    )

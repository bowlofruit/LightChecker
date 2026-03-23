package com.bowlof.lightchecker.data.local

import com.bowlof.lightchecker.data.local.db.OutageSlotEntity
import com.bowlof.lightchecker.domain.model.OutageInterval

internal fun OutageSlotEntity.toDomain(): OutageInterval =
    OutageInterval(startMinute = startMinute, endMinute = endMinute)

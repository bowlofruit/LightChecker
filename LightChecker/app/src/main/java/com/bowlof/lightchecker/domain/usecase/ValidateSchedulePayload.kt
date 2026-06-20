package com.bowlof.lightchecker.domain.usecase

import com.bowlof.lightchecker.domain.model.SyncException

object ValidateSchedulePayload {

    private const val MIN_D = 2020_01_01L
    private const val MAX_D = 2099_12_31L

    fun validate(
        schemaVersion: Int,
        version: Long,
        dayYyyymmdd: Long,
        slotMinutes: List<Int>,
    ): Result<Unit> {
        val reason = when {
            schemaVersion != 1 -> "unsupported_schema_f=$schemaVersion"
            version < 1L -> "invalid_v=$version"
            dayYyyymmdd !in MIN_D..MAX_D -> "invalid_d=$dayYyyymmdd"
            slotMinutes.size % 2 != 0 -> "s_not_pairs"
            else -> firstInvalidSlotReason(slotMinutes)
        }
        return if (reason == null) {
            Result.success(Unit)
        } else {
            Result.failure(SyncException.Parse(reason))
        }
    }

    private fun firstInvalidSlotReason(slotMinutes: List<Int>): String? {
        for (i in slotMinutes.indices step 2) {
            val a = slotMinutes[i]
            val b = slotMinutes[i + 1]
            if (a !in 0..1440 || b !in 0..1440 || a > b) {
                return "invalid_slot_$a,$b"
            }
        }
        return null
    }
}

package com.bowlof.lightchecker.domain.usecase

import com.bowlof.lightchecker.domain.model.OutageInterval

/** Result of computing the user's current position relative to outage intervals. */
sealed interface OutageStatus {
    /** Power is currently off; outage ends at [endsAtMinute]. */
    data class CurrentlyOff(val endsAtMinute: Int) : OutageStatus

    /** Next outage starts in [minutesUntil] minutes, at [startsAtMinute]. */
    data class NextOff(val minutesUntil: Int, val startsAtMinute: Int) : OutageStatus

    data object AllDone : OutageStatus

    /** No schedule data available. */
    data object NoData : OutageStatus
}

object NextOutageCalculator {

    /**
     * Determine the outage status given the sorted [intervals] and the current
     * [nowMinute] (minutes since midnight in Kyiv timezone, 0..1439).
     */
    fun calculate(intervals: List<OutageInterval>, nowMinute: Int): OutageStatus {
        if (intervals.isEmpty()) return OutageStatus.NoData

        for (interval in intervals) {
            if (nowMinute in interval.startMinute until interval.endMinute) {
                return OutageStatus.CurrentlyOff(endsAtMinute = interval.endMinute)
            }
            if (nowMinute < interval.startMinute) {
                return OutageStatus.NextOff(
                    minutesUntil = interval.startMinute - nowMinute,
                    startsAtMinute = interval.startMinute,
                )
            }
        }

        return OutageStatus.AllDone
    }

    fun formatMinute(minute: Int): String {
        val h = minute / 60
        val m = minute % 60
        return "%02d:%02d".format(h, m)
    }
}

package com.bowlof.lightchecker.presentation.util

import com.bowlof.lightchecker.domain.model.OutageInterval
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object OutageIntervalFormatter {

    private val timeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("uk"))

    fun format(interval: OutageInterval): String =
        "${formatMinute(interval.startMinute)}–${formatMinute(interval.endMinute)}"

    private fun formatMinute(minute: Int): String {
        // End-of-day is encoded as 1440; render it as 24:00 instead of collapsing to 23:59.
        if (minute >= MINUTES_PER_DAY) return "24:00"
        val time = LocalTime.ofSecondOfDay(minute.coerceIn(0, MINUTES_PER_DAY - 1) * 60L)
        return time.format(timeFormatter)
    }

    private const val MINUTES_PER_DAY = 24 * 60
}

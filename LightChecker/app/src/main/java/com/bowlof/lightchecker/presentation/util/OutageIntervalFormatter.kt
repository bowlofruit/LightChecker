package com.bowlof.lightchecker.presentation.util

import com.bowlof.lightchecker.domain.model.OutageInterval
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object OutageIntervalFormatter {

    private val timeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("uk"))

    fun format(interval: OutageInterval): String {
        val a = interval.startMinute.coerceIn(0, 24 * 60 - 1)
        val b = interval.endMinute.coerceIn(0, 24 * 60 - 1)
        val ta = LocalTime.ofSecondOfDay(a * 60L)
        val tb = LocalTime.ofSecondOfDay(b * 60L)
        return "${ta.format(timeFormatter)}–${tb.format(timeFormatter)}"
    }
}

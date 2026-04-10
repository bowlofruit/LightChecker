package com.bowlof.lightchecker.presentation.util

import android.content.Context
import com.bowlof.lightchecker.R
import com.bowlof.lightchecker.domain.time.KyivTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Format YYYYMMDD date as human-readable: "Сьогодні", "Завтра", or "4 квітня".
 */
object DateFormatter {

    private val dayMonth = DateTimeFormatter.ofPattern("d MMMM", Locale("uk"))

    fun format(context: Context, yyyymmdd: Long): String {
        val today = KyivTime.todayYyyymmdd()
        val tomorrow = KyivTime.tomorrowYyyymmdd()
        return when (yyyymmdd) {
            today -> context.getString(R.string.schedule_today)
            tomorrow -> context.getString(R.string.schedule_tomorrow)
            else -> {
                val str = yyyymmdd.toString()
                val date = LocalDate.of(
                    str.substring(0, 4).toInt(),
                    str.substring(4, 6).toInt(),
                    str.substring(6, 8).toInt(),
                )
                date.format(dayMonth)
            }
        }
    }
}

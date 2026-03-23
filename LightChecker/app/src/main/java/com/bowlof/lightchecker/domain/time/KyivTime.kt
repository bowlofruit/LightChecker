package com.bowlof.lightchecker.domain.time

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object KyivTime {
    val zone: ZoneId = ZoneId.of("Europe/Kyiv")
    private val ymd = DateTimeFormatter.BASIC_ISO_DATE

    fun localDateNow(clock: Clock = Clock.system(zone)): LocalDate =
        LocalDate.now(clock.withZone(zone))

    fun toYyyymmdd(date: LocalDate): Long = date.format(ymd).toLong()

    fun todayYyyymmdd(clock: Clock = Clock.system(zone)): Long = toYyyymmdd(localDateNow(clock))

    fun tomorrowYyyymmdd(clock: Clock = Clock.system(zone)): Long =
        toYyyymmdd(localDateNow(clock).plusDays(1))
}

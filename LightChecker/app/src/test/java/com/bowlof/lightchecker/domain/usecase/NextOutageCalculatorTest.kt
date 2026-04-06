package com.bowlof.lightchecker.domain.usecase

import com.bowlof.lightchecker.domain.model.OutageInterval
import org.junit.Assert.assertEquals
import org.junit.Test

class NextOutageCalculatorTest {

    @Test
    fun `calculate returns NoData for empty intervals`() {
        val result = NextOutageCalculator.calculate(emptyList(), 600)
        assertEquals(OutageStatus.NoData, result)
    }

    @Test
    fun `calculate returns CurrentlyOff when nowMinute is within an interval`() {
        val intervals = listOf(OutageInterval(startMinute = 60, endMinute = 120))
        val result = NextOutageCalculator.calculate(intervals, nowMinute = 90)
        assertEquals(OutageStatus.CurrentlyOff(endsAtMinute = 120), result)
    }

    @Test
    fun `calculate returns NextOff when nowMinute is before the next interval`() {
        val intervals = listOf(OutageInterval(startMinute = 300, endMinute = 360))
        val result = NextOutageCalculator.calculate(intervals, nowMinute = 200)
        assertEquals(OutageStatus.NextOff(minutesUntil = 100, startsAtMinute = 300), result)
    }

    @Test
    fun `calculate returns AllDone when all intervals have passed`() {
        val intervals = listOf(
            OutageInterval(startMinute = 60, endMinute = 120),
            OutageInterval(startMinute = 180, endMinute = 240),
        )
        val result = NextOutageCalculator.calculate(intervals, nowMinute = 1000)
        assertEquals(OutageStatus.AllDone, result)
    }

    @Test
    fun `formatMinute formats single digit hours and minutes with leading zeros`() {
        assertEquals("05:03", NextOutageCalculator.formatMinute(303))
    }

    @Test
    fun `formatMinute formats midnight as 00 colon 00`() {
        assertEquals("00:00", NextOutageCalculator.formatMinute(0))
    }
}

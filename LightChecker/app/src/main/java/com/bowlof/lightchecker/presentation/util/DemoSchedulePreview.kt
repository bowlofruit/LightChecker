package com.bowlof.lightchecker.presentation.util

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.bowlof.lightchecker.domain.model.OutageInterval

/** Sample outages for debug UI / widget screenshots; toggled via [uiDemoScheduleKey] in app preferences. */
object DemoSchedulePreview {
    val uiDemoScheduleKey = booleanPreferencesKey("ui_demo_schedule")

    val intervals: List<OutageInterval> = listOf(
        OutageInterval(8 * 60 + 30, 11 * 60),
        OutageInterval(14 * 60, 16 * 60 + 30),
        OutageInterval(20 * 60, 22 * 60 + 30),
    )
}

package com.bowlof.lightchecker.presentation.util

import com.bowlof.lightchecker.domain.model.OutageInterval

/** Sample outages for debug UI / widget screenshots; toggle persisted via UiPreferencesRepository. */
object DemoSchedulePreview {

    val intervals: List<OutageInterval> = listOf(
        OutageInterval(8 * 60 + 30, 11 * 60),
        OutageInterval(14 * 60, 16 * 60 + 30),
        OutageInterval(20 * 60, 22 * 60 + 30),
    )
}

package com.bowlof.lightchecker.domain.ids

private val unsafeSegment = Regex("[^a-zA-Z0-9_-]")

object ScheduleDocumentIds {

    fun sanitizeSegment(segment: String): String = segment.replace(unsafeSegment, "_")

    fun firestoreDocumentId(regionId: String, queueId: String): String =
        "${sanitizeSegment(regionId)}__${sanitizeSegment(queueId)}"
}

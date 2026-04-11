package com.bowlof.lightchecker.data.remote

import com.bowlof.lightchecker.data.remote.dto.FirestoreScheduleDto
import com.bowlof.lightchecker.domain.time.KyivTime
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Чистий мапінг полів Firestore → DTO (юніт-тести без DocumentSnapshot).
 */
internal fun firestoreScheduleFieldsToDtoOrNull(
    f: Long?,
    v: Long?,
    d: Long?,
    sRaw: List<*>?,
    g: Long?,
): FirestoreScheduleDto? {
    val fi = f?.toInt() ?: return null
    if (v == null || d == null) return null
    val s = sRaw?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()
    return FirestoreScheduleDto(fi, v, d, s, g)
}

/**
 * Legacy: f=1, кореневі v, d, s.
 * Multi-day: f=2, поле `days` — до двох записів для сьогодні та завтра (Kyiv).
 */
internal fun DocumentSnapshot.toFirestoreScheduleDtosOrEmpty(): List<FirestoreScheduleDto> {
    if (!exists()) return emptyList()
    val f = getLong("f")?.toInt() ?: return emptyList()
    if (f == 2) {
        @Suppress("UNCHECKED_CAST")
        val days = get("days") as? Map<String, Any> ?: return emptyList()
        val today = KyivTime.todayYyyymmdd()
        val tomorrow = KyivTime.tomorrowYyyymmdd()
        return listOfNotNull(
            dayMapEntryToDto(days, today.toString()),
            dayMapEntryToDto(days, tomorrow.toString()),
        )
    }
    val single = toFirestoreScheduleDtoOrNull()
    return if (single != null) listOf(single) else emptyList()
}

private fun dayMapEntryToDto(days: Map<String, Any>, key: String): FirestoreScheduleDto? {
    val raw = days[key] as? Map<*, *> ?: return null
    val v = (raw["v"] as? Number)?.toLong() ?: return null
    @Suppress("UNCHECKED_CAST")
    val sList = raw["s"] as? List<*>
    val s = sList?.mapNotNull { (it as? Number)?.toInt() } ?: return null
    val d = key.toLongOrNull() ?: return null
    val g = (raw["g"] as? Number)?.toLong()
    return FirestoreScheduleDto(1, v, d, s, g)
}

internal fun DocumentSnapshot.toFirestoreScheduleDtoOrNull(): FirestoreScheduleDto? {
    if (!exists()) return null
    @Suppress("UNCHECKED_CAST")
    val raw = get("s") as? List<*>
    return firestoreScheduleFieldsToDtoOrNull(getLong("f"), getLong("v"), getLong("d"), raw, getLong("g"))
}

package com.bowlof.lightchecker.data.remote

import com.bowlof.lightchecker.data.remote.dto.FirestoreScheduleDto
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

internal fun DocumentSnapshot.toFirestoreScheduleDtoOrNull(): FirestoreScheduleDto? {
    if (!exists()) return null
    @Suppress("UNCHECKED_CAST")
    val raw = get("s") as? List<*>
    return firestoreScheduleFieldsToDtoOrNull(getLong("f"), getLong("v"), getLong("d"), raw, getLong("g"))
}

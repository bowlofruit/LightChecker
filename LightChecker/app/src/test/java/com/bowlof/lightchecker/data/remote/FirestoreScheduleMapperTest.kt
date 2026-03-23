package com.bowlof.lightchecker.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirestoreScheduleMapperTest {

    @Test
    fun `maps long fields and int list`() {
        val dto = firestoreScheduleFieldsToDtoOrNull(
            f = 1L,
            v = 2L,
            d = 20260323L,
            sRaw = listOf(480, 600),
            g = 100L,
        )
        requireNotNull(dto)
        assertEquals(1, dto.f)
        assertEquals(2L, dto.v)
        assertEquals(20260323L, dto.d)
        assertEquals(listOf(480, 600), dto.s)
        assertEquals(100L, dto.g)
    }

    @Test
    fun `null when f missing`() {
        assertNull(
            firestoreScheduleFieldsToDtoOrNull(null, 1L, 20260323L, listOf(0, 1), null),
        )
    }

    @Test
    fun `null when v or d missing`() {
        assertNull(firestoreScheduleFieldsToDtoOrNull(1L, null, 20260323L, emptyList<Any>(), null))
        assertNull(firestoreScheduleFieldsToDtoOrNull(1L, 1L, null, emptyList<Any>(), null))
    }

    @Test
    fun `coerces numeric elements in s`() {
        val dto = firestoreScheduleFieldsToDtoOrNull(
            1L,
            1L,
            20260323L,
            listOf(1.0, 2L, 3.0f),
            null,
        )
        requireNotNull(dto)
        assertEquals(listOf(1, 2, 3), dto.s)
    }
}

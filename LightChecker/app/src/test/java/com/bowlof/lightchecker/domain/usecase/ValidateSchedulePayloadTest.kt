package com.bowlof.lightchecker.domain.usecase

import com.bowlof.lightchecker.domain.model.SyncException
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateSchedulePayloadTest {

    @Test
    fun `rejects odd s length`() {
        val r = ValidateSchedulePayload.validate(1, 1L, 20260323L, listOf(0, 60, 120))
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull() is SyncException.Parse)
    }

    @Test
    fun `accepts valid payload`() {
        val r = ValidateSchedulePayload.validate(1, 1L, 20260323L, listOf(480, 600))
        assertTrue(r.isSuccess)
    }

    @Test
    fun `rejects wrong schema f`() {
        val r = ValidateSchedulePayload.validate(999, 1L, 20260323L, listOf(0, 1))
        assertTrue(r.isFailure)
    }

    @Test
    fun `rejects single element s`() {
        val r = ValidateSchedulePayload.validate(1, 1L, 20260323L, listOf(1))
        assertTrue(r.isFailure)
    }

    @Test
    fun `rejects invalid d below range`() {
        val r = ValidateSchedulePayload.validate(1, 1L, 20191231L, listOf(0, 1))
        assertTrue(r.isFailure)
    }

    @Test
    fun `rejects slot start after end`() {
        val r = ValidateSchedulePayload.validate(1, 1L, 20260323L, listOf(120, 60))
        assertTrue(r.isFailure)
    }

    @Test
    fun `rejects non-positive version`() {
        val r = ValidateSchedulePayload.validate(1, 0L, 20260323L, listOf(480, 600))
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull() is SyncException.Parse)
    }
}

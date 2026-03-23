package com.bowlof.lightchecker

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicLong

/**
 * Мітка часу для NFR: тривалість до першого кадру (debug, `BuildConfig.LOG_STARTUP_TIMING`).
 */
object StartupMetrics {

    private val processStartElapsed = AtomicLong(0L)

    fun markProcessStartIfUnset() {
        processStartElapsed.compareAndSet(0L, SystemClock.elapsedRealtime())
    }

    fun processStartElapsedRealtimeMs(): Long = processStartElapsed.get()
}

package com.bowlof.lightchecker.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bowlof.lightchecker.domain.repository.ScheduleRepository
import com.bowlof.lightchecker.widget.OutageGlanceAppWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class SyncScheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scheduleRepository: ScheduleRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val r = inputData.getString(KEY_REGION) ?: return Result.failure()
        val q = inputData.getString(KEY_QUEUE) ?: return Result.failure()
        val v = inputData.getString(KEY_VERSION)?.toLongOrNull()
        val d = inputData.getString(KEY_DAY)?.toLongOrNull()
        return try {
            scheduleRepository.syncIfNewerVersion(r, q, v, d)
            runCatching { OutageGlanceAppWidget().updateAll(applicationContext) }
            Result.success()
        } catch (e: Exception) {
            Timber.w(e, "SyncScheduleWorker attempt=%s", runAttemptCount)
            if (runAttemptCount < 4) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_REGION = "r"
        const val KEY_QUEUE = "q"
        const val KEY_VERSION = "v"
        const val KEY_DAY = "d"
        const val UNIQUE_NAME_PREFIX = "sync_schedule_"
    }
}

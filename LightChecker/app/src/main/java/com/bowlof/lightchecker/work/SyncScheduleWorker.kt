package com.bowlof.lightchecker.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bowlof.lightchecker.LightCheckerApplication
import com.bowlof.lightchecker.MainActivity
import com.bowlof.lightchecker.R
import com.bowlof.lightchecker.data.local.db.LightCheckerDatabase
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
    private val database: LightCheckerDatabase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val r = inputData.getString(KEY_REGION) ?: return Result.failure()
        val q = inputData.getString(KEY_QUEUE) ?: return Result.failure()
        val v = inputData.getString(KEY_VERSION)?.toLongOrNull()
        val d = inputData.getString(KEY_DAY)?.toLongOrNull()
        return try {
            scheduleRepository.syncIfNewerVersion(r, q, v, d)
            runCatching { OutageGlanceAppWidget().updateAll(applicationContext) }
            showNotificationIfAllowed(r, q)
            Result.success()
        } catch (e: Exception) {
            Timber.w(e, "SyncScheduleWorker attempt=%s", runAttemptCount)
            if (runAttemptCount < 4) Result.retry() else Result.failure()
        }
    }

    private suspend fun showNotificationIfAllowed(regionId: String, queueId: String) {
        if (!hasNotificationPermission()) return

        val place = database.savedLocationDao().getAllSnapshot()
            .find { it.regionId == regionId && it.queueId == queueId }
            ?: return
        if (!place.notificationsEnabled) return

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(EXTRA_REGION, regionId)
            putExtra(EXTRA_QUEUE, queueId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            (regionId + queueId).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            LightCheckerApplication.NOTIFICATION_CHANNEL_ID,
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.schedule_title))
            .setContentText(
                applicationContext.getString(
                    R.string.notification_schedule_updated,
                    place.cityDisplayName,
                ),
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(applicationContext)
                .notify(NOTIFICATION_ID_BASE + place.id.toInt(), notification)
        }
    }

    private fun hasNotificationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val KEY_REGION = "r"
        const val KEY_QUEUE = "q"
        const val KEY_VERSION = "v"
        const val KEY_DAY = "d"
        const val UNIQUE_NAME_PREFIX = "sync_schedule_"
        const val EXTRA_REGION = "notification_region"
        const val EXTRA_QUEUE = "notification_queue"
        private const val NOTIFICATION_ID_BASE = 1000
    }
}

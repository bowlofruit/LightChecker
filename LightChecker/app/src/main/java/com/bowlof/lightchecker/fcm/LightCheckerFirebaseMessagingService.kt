package com.bowlof.lightchecker.fcm

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bowlof.lightchecker.work.SyncScheduleWorker
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber
import java.util.concurrent.TimeUnit

class LightCheckerFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val r = data["r"] ?: return
        val q = data["q"] ?: return
        val v = data["v"]
        val d = data["d"]
        Timber.d("FCM data r=%s q=%s v=%s d=%s", r, q, v, d)

        val input = Data.Builder()
            .putString(SyncScheduleWorker.KEY_REGION, r)
            .putString(SyncScheduleWorker.KEY_QUEUE, q)
        if (v != null) input.putString(SyncScheduleWorker.KEY_VERSION, v)
        if (d != null) input.putString(SyncScheduleWorker.KEY_DAY, d)

        val work = OneTimeWorkRequestBuilder<SyncScheduleWorker>()
            .setInputData(input.build())
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30_000L, TimeUnit.MILLISECONDS)
            .build()

        val name = "${SyncScheduleWorker.UNIQUE_NAME_PREFIX}${r}_$q"
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            name,
            ExistingWorkPolicy.KEEP,
            work,
        )
    }
}

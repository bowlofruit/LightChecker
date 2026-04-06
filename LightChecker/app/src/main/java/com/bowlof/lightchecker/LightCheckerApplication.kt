package com.bowlof.lightchecker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.SystemClock
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class LightCheckerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        StartupMetrics.markProcessStartIfUnset()
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.tag("NFR").d("app_onCreate_elapsedRealtimeMs=%d", SystemClock.elapsedRealtime())
        }
        runCatching {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        }
        createNotificationChannel()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "schedule_updates"
    }
}

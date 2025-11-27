package com.Bowlof.LightChecker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object SharedData {
    private const val PREFS = "LightCheckerPrefs"

    @JvmStatic
    fun saveSettings(context: Context, url: String, regex: String, title: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("data_url", url)
            .putString("parsing_regex", regex)
            .putString("widget_title", title)
            .apply()
        
        forceUpdate(context)
    }

    @JvmStatic
    fun forceUpdate(context: Context) {
        val request = OneTimeWorkRequestBuilder<ScheduleWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }

    fun schedulePeriodicWork(context: Context) {
        val request = PeriodicWorkRequestBuilder<ScheduleWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "LightCheckerUpdate",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
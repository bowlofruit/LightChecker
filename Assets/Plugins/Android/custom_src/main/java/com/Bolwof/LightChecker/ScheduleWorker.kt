package com.Bowlof.LightChecker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("PowerSchedulePrefs", Context.MODE_PRIVATE)
        
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        
        prefs.edit()
            .putString("cached_content", "Test Update Success!\nTime: $currentTime")
            .putString("last_update", currentTime)
            .apply()

        updateWidgetUI()
        
        return Result.success()
    }

    private fun updateWidgetUI() {
        val intent = Intent(applicationContext, LightWidgetProvider::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(applicationContext)
            .getAppWidgetIds(ComponentName(applicationContext, LightWidgetProvider::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        applicationContext.sendBroadcast(intent)
    }
}
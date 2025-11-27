package com.Bowlof.LightChecker

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class LightWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        SharedData.schedulePeriodicWork(context)
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences("LightCheckerPrefs", Context.MODE_PRIVATE)
            val title = prefs.getString("widget_title", "Light Checker")
            val content = prefs.getString("cached_content", "Очікування даних...")
            val time = prefs.getString("last_update", "--:--")

            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_content, content)
            views.setTextViewText(R.id.widget_update_time, time)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
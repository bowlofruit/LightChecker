package com.bowlof.lightchecker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.compose.ui.unit.dp
import androidx.glance.layout.Column
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.bowlof.lightchecker.R
import com.bowlof.lightchecker.di.GlanceWidgetEntryPoint
import com.bowlof.lightchecker.domain.model.OutageInterval
import com.bowlof.lightchecker.domain.time.KyivTime
import com.bowlof.lightchecker.presentation.util.OutageIntervalFormatter
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class OutageGlanceAppWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lines = loadLines(context)
        provideContent {
            WidgetContent(lines)
        }
    }

    private suspend fun loadLines(context: Context): List<String> = withContext(Dispatchers.IO) {
        val db = EntryPointAccessors.fromApplication(
            context.applicationContext,
            GlanceWidgetEntryPoint::class.java,
        ).database()
        val primary = db.savedLocationDao().observePrimary().first()
            ?: return@withContext listOf(context.getString(R.string.widget_no_place))
        val today = KyivTime.todayYyyymmdd()
        val tomorrow = KyivTime.tomorrowYyyymmdd()
        val todaySlots = db.outageSlotDao()
            .observeSlots(primary.regionId, primary.queueId, today)
            .first()
        val useTomorrow = todaySlots.isEmpty()
        val d = if (useTomorrow) tomorrow else today
        val slots = if (useTomorrow) {
            db.outageSlotDao().observeSlots(primary.regionId, primary.queueId, tomorrow).first()
        } else {
            todaySlots
        }
        val header = buildString {
            append(primary.queueDisplayName)
            append(" · ")
            append(d)
            if (useTomorrow && slots.isNotEmpty()) {
                append(" (")
                append(context.getString(R.string.widget_tomorrow_label))
                append(")")
            }
        }
        if (slots.isEmpty()) {
            listOf(header, context.getString(R.string.widget_empty_day))
        } else {
            buildList {
                add(header)
                slots.forEach { e ->
                    add(
                        OutageIntervalFormatter.format(
                            OutageInterval(e.startMinute, e.endMinute),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetContent(lines: List<String>) {
    Column(modifier = GlanceModifier.padding(8.dp)) {
        lines.forEach { line ->
            Text(text = line, modifier = GlanceModifier.padding(bottom = 4.dp))
        }
    }
}

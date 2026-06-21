package com.bowlof.lightchecker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.bowlof.lightchecker.BuildConfig
import com.bowlof.lightchecker.MainActivity
import com.bowlof.lightchecker.R
import com.bowlof.lightchecker.di.GlanceWidgetEntryPoint
import com.bowlof.lightchecker.domain.model.OutageInterval
import com.bowlof.lightchecker.domain.time.KyivTime
import com.bowlof.lightchecker.presentation.util.DateFormatter
import com.bowlof.lightchecker.presentation.util.DemoSchedulePreview
import com.bowlof.lightchecker.presentation.util.OutageIntervalFormatter
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val DarkBlue = androidx.compose.ui.graphics.Color(0xFF1E3A5F)
private val Amber = androidx.compose.ui.graphics.Color(0xFFF59E0B)
private val White = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
private val DarkSurface = androidx.compose.ui.graphics.Color(0xFF1C1917)
private val LightText = androidx.compose.ui.graphics.Color(0xFFE6E1D9)
private val Slate400 = androidx.compose.ui.graphics.Color(0xFF94A3B8)

private val SHOW_TOMORROW_KEY = booleanPreferencesKey("show_tomorrow")
private val TOMORROW_PARAM = ActionParameters.Key<Boolean>("tomorrow")

class OutageGlanceAppWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val todayData = loadData(context, showTomorrow = false)
        val tomorrowData = loadData(context, showTomorrow = true)

        provideContent {
            val showTomorrow = currentState(SHOW_TOMORROW_KEY) ?: false
            val data = if (showTomorrow) tomorrowData else todayData
            WidgetContent(data, showTomorrow)
        }
    }

    private suspend fun loadData(context: Context, showTomorrow: Boolean): WidgetData =
        withContext(Dispatchers.IO) {
            val entry = EntryPointAccessors.fromApplication(
                context.applicationContext,
                GlanceWidgetEntryPoint::class.java,
            )
            val db = entry.database()
            val demoSchedule =
                BuildConfig.DEBUG &&
                    entry.preferencesDataStore().data.first()[DemoSchedulePreview.uiDemoScheduleKey] == true

            val primary = db.savedLocationDao().observePrimary().first()
                ?: return@withContext WidgetData(
                    title = context.getString(R.string.widget_no_place),
                    subtitle = null,
                    intervals = emptyList(),
                )

            val targetDay = if (showTomorrow) KyivTime.tomorrowYyyymmdd() else KyivTime.todayYyyymmdd()
            val slots = db.outageSlotDao()
                .observeSlots(primary.regionId, primary.queueId, targetDay)
                .first()

            val intervalStrings = if (demoSchedule) {
                DemoSchedulePreview.intervals.map { OutageIntervalFormatter.format(it) }
            } else {
                slots.map {
                    OutageIntervalFormatter.format(OutageInterval(it.startMinute, it.endMinute))
                }
            }
            val emptyMessage = if (!demoSchedule && slots.isEmpty()) {
                context.getString(R.string.widget_empty_day)
            } else {
                null
            }

            WidgetData(
                title = "${primary.cityDisplayName} · ${primary.queueDisplayName}",
                subtitle = DateFormatter.format(context, targetDay),
                intervals = intervalStrings,
                emptyMessage = emptyMessage,
            )
        }
}

private data class WidgetData(
    val title: String,
    val subtitle: String?,
    val intervals: List<String>,
    val emptyMessage: String? = null,
)

@Composable
private fun WidgetContent(data: WidgetData, showTomorrow: Boolean) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(DarkBlue, DarkSurface))
            .cornerRadius(16.dp)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(
            text = data.title,
            style = TextStyle(
                color = ColorProvider(Amber, Amber),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
        )

        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (data.subtitle != null) {
                Text(
                    text = data.subtitle,
                    style = TextStyle(color = ColorProvider(Slate400, Slate400), fontSize = 12.sp),
                )
            }
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = if (showTomorrow) " \u25C0 " else " \u25B6 ",
                modifier = GlanceModifier
                    .cornerRadius(8.dp)
                    .clickable(
                        actionRunCallback<ToggleDayAction>(
                            actionParametersOf(TOMORROW_PARAM to !showTomorrow),
                        ),
                    ),
                style = TextStyle(
                    color = ColorProvider(Amber, Amber),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        if (data.emptyMessage != null) {
            Text(
                text = data.emptyMessage,
                style = TextStyle(color = ColorProvider(White, LightText), fontSize = 13.sp),
            )
        } else {
            data.intervals.forEach { interval ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "\u26A1", style = TextStyle(fontSize = 12.sp))
                    Text(
                        text = "  $interval",
                        style = TextStyle(
                            color = ColorProvider(White, LightText),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}

class ToggleDayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val tomorrow = parameters[TOMORROW_PARAM] ?: false
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[SHOW_TOMORROW_KEY] = tomorrow
        }
        OutageGlanceAppWidget().update(context, glanceId)
    }
}

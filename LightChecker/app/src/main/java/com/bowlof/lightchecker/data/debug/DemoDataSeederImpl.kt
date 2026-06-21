package com.bowlof.lightchecker.data.debug

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import androidx.room.withTransaction
import com.bowlof.lightchecker.LightCheckerApplication
import com.bowlof.lightchecker.R
import com.bowlof.lightchecker.data.local.db.LightCheckerDatabase
import com.bowlof.lightchecker.data.local.db.OutageSlotEntity
import com.bowlof.lightchecker.data.local.db.SavedLocationEntity
import com.bowlof.lightchecker.data.local.db.SyncHistoryEntity
import com.bowlof.lightchecker.data.local.db.SyncMetaEntity
import com.bowlof.lightchecker.domain.debug.DemoDataSeeder
import com.bowlof.lightchecker.domain.model.LocationSource
import com.bowlof.lightchecker.domain.time.KyivTime
import com.bowlof.lightchecker.widget.OutageGlanceAppWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("MagicNumber")
class DemoDataSeederImpl @Inject constructor(
    private val database: LightCheckerDatabase,
    @ApplicationContext private val context: Context,
) : DemoDataSeeder {

    private data class DemoPlace(
        val cityId: String,
        val queueId: String,
        val cityName: String,
        val queueName: String,
        val primary: Boolean,
    )

    override suspend fun seed() = withContext(Dispatchers.IO) {
        val today = KyivTime.todayYyyymmdd()
        val tomorrow = KyivTime.tomorrowYyyymmdd()
        val now = ZonedDateTime.now(KyivTime.zone)
        val nowMin = now.hour * 60 + now.minute
        val nowMillis = System.currentTimeMillis()

        val places = listOf(
            DemoPlace("kyiv", "2.1", "Київ", "Черга 2.1", primary = true),
            DemoPlace("lviv", "1.2", "Львів", "Черга 1.2", primary = false),
            DemoPlace("cherkasy", "3.2", "Черкаси", "Черга 3.2", primary = false),
        )

        val locationDao = database.savedLocationDao()
        database.withTransaction {
            locationDao.getAllSnapshot().forEach { locationDao.delete(it) }
            database.outageSlotDao().deleteOlderThan(Long.MAX_VALUE)
            database.syncMetaDao().deleteOlderThan(Long.MAX_VALUE)
            database.syncHistoryDao().pruneOldEntries(0)

            places.forEachIndexed { index, place ->
                locationDao.insert(
                    SavedLocationEntity(
                        regionId = place.cityId,
                        queueId = place.queueId,
                        cityId = place.cityId,
                        cityDisplayName = place.cityName,
                        queueDisplayName = place.queueName,
                        sortOrder = index,
                        isWidgetPrimary = place.primary,
                        latitude = null,
                        longitude = null,
                        locationSource = LocationSource.USER_MANUAL.name,
                        notificationsEnabled = true,
                    ),
                )
                insertDay(place.cityId, place.queueId, today, todayIntervals(index, nowMin), version = 3, nowMillis)
                insertDay(place.cityId, place.queueId, tomorrow, tomorrowIntervals(index), version = 1, nowMillis)
                seedHistory(place.cityId, place.queueId, today, place.cityName, nowMillis)
            }
        }

        runCatching { OutageGlanceAppWidget().updateAll(context) }
        postDemoNotification(places.first { it.primary }.cityName)
    }

    private suspend fun insertDay(
        regionId: String,
        queueId: String,
        day: Long,
        intervals: List<Pair<Int, Int>>,
        version: Long,
        nowMillis: Long,
    ) {
        val slots = intervals.mapIndexed { i, (start, end) ->
            OutageSlotEntity(regionId, queueId, day, i, start, end)
        }
        if (slots.isNotEmpty()) database.outageSlotDao().insertAll(slots)
        database.syncMetaDao().upsert(SyncMetaEntity(regionId, queueId, day, version, nowMillis))
    }

    private suspend fun seedHistory(
        regionId: String,
        queueId: String,
        day: Long,
        cityName: String,
        nowMillis: Long,
    ) {
        val rows = listOf(
            Triple(null as Long?, 1L, nowMillis - 3 * HOUR_MS),
            Triple(1L, 2L, nowMillis - 90 * MINUTE_MS),
            Triple(2L, 3L, nowMillis - 20 * MINUTE_MS),
        )
        rows.forEach { (old, new, at) ->
            database.syncHistoryDao().insert(
                SyncHistoryEntity(
                    regionId = regionId,
                    queueId = queueId,
                    dateYyyymmdd = day,
                    oldVersion = old,
                    newVersion = new,
                    syncedAtEpochMillis = at,
                    cityDisplayName = cityName,
                ),
            )
        }
    }

    /** Today's pattern, anchored to [nowMin] so the primary place is "currently off" now. */
    private fun todayIntervals(index: Int, nowMin: Int): List<Pair<Int, Int>> = when (index) {
        0 -> listOfNotNull(clamp(nowMin - 45, nowMin + 135), clamp(nowMin + 300, nowMin + 450))
        1 -> listOfNotNull(clamp(nowMin + 75, nowMin + 255))
        else -> listOf(420 to 600, 1140 to 1320)
    }

    private fun tomorrowIntervals(index: Int): List<Pair<Int, Int>> = when (index) {
        0 -> listOf(480 to 690, 1020 to 1230)
        1 -> listOf(600 to 780)
        else -> listOf(300 to 480, 900 to 1080)
    }

    private fun clamp(start: Int, end: Int): Pair<Int, Int>? {
        val s = start.coerceIn(0, MINUTES_PER_DAY)
        val e = end.coerceIn(0, MINUTES_PER_DAY)
        return if (s < e) s to e else null
    }

    private fun postDemoNotification(cityName: String) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val notification = NotificationCompat.Builder(
            context,
            LightCheckerApplication.NOTIFICATION_CHANNEL_ID,
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.schedule_title))
            .setContentText(context.getString(R.string.notification_schedule_updated, cityName))
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(DEMO_NOTIFICATION_ID, notification)
        }
    }

    private companion object {
        const val MINUTES_PER_DAY = 24 * 60
        const val HOUR_MS = 3_600_000L
        const val MINUTE_MS = 60_000L
        const val DEMO_NOTIFICATION_ID = 9999
    }
}

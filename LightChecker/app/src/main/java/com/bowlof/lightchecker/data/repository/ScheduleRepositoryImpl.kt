package com.bowlof.lightchecker.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.room.withTransaction
import com.bowlof.lightchecker.data.local.db.LightCheckerDatabase
import com.bowlof.lightchecker.data.local.db.OutageSlotEntity
import com.bowlof.lightchecker.data.local.db.SyncEventEntity
import com.bowlof.lightchecker.data.local.db.SyncHistoryEntity
import com.bowlof.lightchecker.data.local.db.SyncMetaEntity
import com.bowlof.lightchecker.data.local.toDomain
import com.bowlof.lightchecker.data.remote.FirestoreScheduleDataSource
import com.bowlof.lightchecker.data.remote.dto.FirestoreScheduleDto
import com.bowlof.lightchecker.domain.ids.ScheduleDocumentIds
import com.bowlof.lightchecker.domain.model.DaySchedule
import com.bowlof.lightchecker.domain.model.OutageInterval
import com.bowlof.lightchecker.domain.model.SyncException
import com.bowlof.lightchecker.domain.repository.ScheduleRepository
import com.bowlof.lightchecker.domain.time.KyivTime
import com.bowlof.lightchecker.domain.usecase.ValidateSchedulePayload
import com.bowlof.lightchecker.widget.OutageGlanceAppWidget
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LightCheckerDatabase,
    private val remote: FirestoreScheduleDataSource,
) : ScheduleRepository {

    private val outageDao get() = database.outageSlotDao()
    private val syncDao get() = database.syncMetaDao()
    private val historyDao get() = database.syncHistoryDao()
    private val eventDao get() = database.syncEventDao()

    override fun observeIntervals(
        regionId: String,
        queueId: String,
        effectiveDateYyyymmdd: Long,
    ): Flow<List<OutageInterval>> {
        return outageDao.observeSlots(regionId, queueId, effectiveDateYyyymmdd)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun observeDaySchedule(
        regionId: String,
        queueId: String,
        effectiveDateYyyymmdd: Long,
    ): Flow<DaySchedule> {
        return combine(
            outageDao.observeSlots(regionId, queueId, effectiveDateYyyymmdd)
                .map { list -> list.map { it.toDomain() } },
            syncDao.observeMeta(regionId, queueId, effectiveDateYyyymmdd),
        ) { intervals, meta ->
            DaySchedule(
                effectiveDateYyyymmdd = effectiveDateYyyymmdd,
                cachedVersion = meta?.cachedVersion,
                intervals = intervals,
                lastSyncAtEpochMillis = meta?.lastSyncSuccessAtEpochMillis,
            )
        }.distinctUntilChanged()
    }

    override suspend fun refreshSchedule(regionId: String, queueId: String) {
        val docId = ScheduleDocumentIds.firestoreDocumentId(regionId, queueId)
        val dtos = try {
            remote.fetchSchedules(docId)
        } catch (e: FirebaseFirestoreException) {
            Timber.w(e, "firestore fetch")
            throw SyncException.Unknown(e)
        } catch (e: Exception) {
            Timber.w(e, "firestore fetch")
            throw SyncException.Network(e)
        }
        if (dtos.isEmpty()) return

        val today = KyivTime.todayYyyymmdd()
        val tomorrow = KyivTime.tomorrowYyyymmdd()
        val window = setOf(today, tomorrow)
        val syncedDays = dtos.map { it.d }.toSet()

        val oldMetas = dtos.associate { dto ->
            dto.d to syncDao.get(regionId, queueId, dto.d)
        }

        for (dto in dtos) {
            ValidateSchedulePayload.validate(dto.f, dto.v, dto.d, dto.s).getOrThrow()
        }

        database.withTransaction {
            for (d in window) {
                if (d !in syncedDays) {
                    outageDao.deleteForDay(regionId, queueId, d)
                    syncDao.deleteForDay(regionId, queueId, d)
                }
            }
            for (dto in dtos) {
                val slots = slotsFromDto(regionId, queueId, dto)
                val meta = SyncMetaEntity(
                    regionId = regionId,
                    queueId = queueId,
                    effectiveDateYyyymmdd = dto.d,
                    cachedVersion = dto.v,
                    lastSyncSuccessAtEpochMillis = System.currentTimeMillis(),
                )
                outageDao.deleteForDay(regionId, queueId, dto.d)
                if (slots.isNotEmpty()) {
                    outageDao.insertAll(slots)
                }
                syncDao.upsert(meta)
            }
        }

        for (dto in dtos) {
            historyDao.insert(
                SyncHistoryEntity(
                    regionId = regionId,
                    queueId = queueId,
                    dateYyyymmdd = dto.d,
                    oldVersion = oldMetas[dto.d]?.cachedVersion,
                    newVersion = dto.v,
                    syncedAtEpochMillis = System.currentTimeMillis(),
                ),
            )
            eventDao.insert(
                SyncEventEntity(
                    timestampMillis = System.currentTimeMillis(),
                    eventType = "SYNC_SUCCESS",
                    regionId = regionId,
                    queueId = queueId,
                    details = "v=${dto.v} day=${dto.d}",
                ),
            )
        }
        historyDao.pruneOldEntries()
        eventDao.pruneOldEntries()

        purgeStaleCache()
        runCatching { OutageGlanceAppWidget().updateAll(context) }
            .onFailure { Timber.w(it, "glance update") }
    }

    private fun slotsFromDto(
        regionId: String,
        queueId: String,
        dto: FirestoreScheduleDto,
    ): List<OutageSlotEntity> {
        return buildList {
            var pairIndex = 0
            var i = 0
            while (i + 1 < dto.s.size) {
                add(
                    OutageSlotEntity(
                        regionId = regionId,
                        queueId = queueId,
                        effectiveDateYyyymmdd = dto.d,
                        slotIndex = pairIndex,
                        startMinute = dto.s[i],
                        endMinute = dto.s[i + 1],
                    ),
                )
                pairIndex++
                i += 2
            }
        }
    }

    override suspend fun syncIfNewerVersion(
        regionId: String,
        queueId: String,
        remoteVersion: Long?,
        remoteDay: Long?,
    ) {
        if (remoteVersion != null && remoteDay != null) {
            val meta = syncDao.get(regionId, queueId, remoteDay)
            if (meta != null && meta.cachedVersion >= remoteVersion) {
                Timber.d("sync skip up-to-date r=$regionId q=$queueId d=$remoteDay v=$remoteVersion")
                eventDao.insert(
                    SyncEventEntity(
                        timestampMillis = System.currentTimeMillis(),
                        eventType = "SYNC_SKIPPED",
                        regionId = regionId,
                        queueId = queueId,
                        details = "cached v=$remoteVersion d=$remoteDay",
                    ),
                )
                return
            }
        }
        refreshSchedule(regionId, queueId)
    }

    override suspend fun purgeStaleCache() {
        val today = KyivTime.todayYyyymmdd()
        outageDao.deleteOlderThan(today)
        syncDao.deleteOlderThan(today)
    }
}

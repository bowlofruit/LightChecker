package com.bowlof.lightchecker.data.repository

import androidx.room.withTransaction
import com.bowlof.lightchecker.data.local.toDomain
import com.bowlof.lightchecker.data.local.db.LightCheckerDatabase
import com.bowlof.lightchecker.data.local.db.SavedLocationEntity
import com.bowlof.lightchecker.data.messaging.FirebaseTopicManager
import com.bowlof.lightchecker.domain.model.LocationSource
import com.bowlof.lightchecker.domain.model.SavedPlace
import com.bowlof.lightchecker.domain.repository.LocationsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationsRepositoryImpl @Inject constructor(
    private val database: LightCheckerDatabase,
    private val topicManager: FirebaseTopicManager,
) : LocationsRepository {

    private val dao get() = database.savedLocationDao()

    override fun observeSavedPlaces(): Flow<List<SavedPlace>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeSavedPlaceCount(): Flow<Int> = dao.observeCount()

    override fun observePrimaryPlace(): Flow<SavedPlace?> =
        dao.observePrimary().map { it?.toDomain() }

    override suspend fun addPlace(
        regionId: String,
        queueId: String,
        cityId: String,
        cityDisplayName: String,
        queueDisplayName: String,
        latitude: Double?,
        longitude: Double?,
        locationSource: LocationSource?,
        setAsWidgetPrimary: Boolean,
    ): Long {
        val id = database.withTransaction {
            if (setAsWidgetPrimary) {
                dao.clearAllPrimaryFlags()
            }
            val nextOrder = (dao.maxSortOrder() ?: -1) + 1
            val entity = SavedLocationEntity(
                regionId = regionId,
                queueId = queueId,
                cityId = cityId,
                cityDisplayName = cityDisplayName,
                queueDisplayName = queueDisplayName,
                sortOrder = nextOrder,
                isWidgetPrimary = setAsWidgetPrimary,
                latitude = latitude,
                longitude = longitude,
                locationSource = locationSource?.name,
            )
            dao.insert(entity)
        }
        topicManager.syncSubscriptionsAfterDataChange()
        return id
    }

    override suspend fun deletePlace(id: Long) {
        val row = dao.getById(id) ?: return
        dao.delete(row)
        topicManager.syncSubscriptionsAfterDataChange()
    }

    override suspend fun setWidgetPrimary(id: Long) {
        database.withTransaction {
            dao.clearAllPrimaryFlags()
            val row = dao.getById(id) ?: return@withTransaction
            dao.update(row.copy(isWidgetPrimary = true))
        }
        topicManager.syncSubscriptionsAfterDataChange()
    }

    override suspend fun setNotificationsEnabled(id: Long, enabled: Boolean) {
        dao.setNotificationsEnabled(id, enabled)
    }

    override suspend fun swapSortOrder(idA: Long, idB: Long) {
        database.withTransaction {
            val a = dao.getById(idA) ?: return@withTransaction
            val b = dao.getById(idB) ?: return@withTransaction
            dao.updateSortOrder(idA, b.sortOrder)
            dao.updateSortOrder(idB, a.sortOrder)
        }
    }
}

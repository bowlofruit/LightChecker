package com.bowlof.lightchecker.data.catalog

import android.content.Context
import com.bowlof.lightchecker.domain.catalog.CityCatalogProvider
import com.bowlof.lightchecker.domain.model.CatalogCity
import com.bowlof.lightchecker.domain.model.CatalogQueue
import com.bowlof.lightchecker.domain.model.CityCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CitiesCatalogLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) : CityCatalogProvider {

    override suspend fun load(): Result<CityCatalog> = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val version = root.getInt("catalogVersion")
            val citiesJson = root.getJSONArray("cities")
            val cities = buildList {
                for (i in 0 until citiesJson.length()) {
                    val c = citiesJson.getJSONObject(i)
                    val queuesJson = c.getJSONArray("queues")
                    val queues = buildList {
                        for (j in 0 until queuesJson.length()) {
                            val q = queuesJson.getJSONObject(j)
                            add(
                                CatalogQueue(
                                    queueId = q.getString("queueId"),
                                    displayName = q.getString("displayName"),
                                    regionId = q.getString("regionId"),
                                ),
                            )
                        }
                    }
                    add(
                        CatalogCity(
                            cityId = c.getString("cityId"),
                            displayName = c.getString("displayName"),
                            queues = queues,
                        ),
                    )
                }
            }
            CityCatalog(version, cities)
        }
    }

    companion object {
        private const val ASSET_NAME = "cities_queues.json"
    }
}

package com.bowlof.lightchecker.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class ResolvedLocationLabel(
    val latitude: Double,
    val longitude: Double,
    val locality: String?,
)

@Singleton
class DeviceLocationReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val fused by lazy { LocationServices.getFusedLocationProviderClient(context) }

    @SuppressLint("MissingPermission")
    suspend fun getLastLocationOrNull(): ResolvedLocationLabel? = withContext(Dispatchers.IO) {
        val loc = runCatching { fused.lastLocation.await() }.getOrNull() ?: return@withContext null
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = runCatching {
            geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
        }.getOrNull()
        val locality = addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.subAdminArea
        ResolvedLocationLabel(loc.latitude, loc.longitude, locality)
    }
}

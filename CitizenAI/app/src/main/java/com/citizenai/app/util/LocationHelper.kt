package com.citizenai.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed class LocationResult {
    data class Success(val latitude: Double, val longitude: Double, val address: String) : LocationResult()
    data class Error(val message: String) : LocationResult()
}

@Singleton
class LocationHelper @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient
) {
    fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): LocationResult {
        if (!hasLocationPermission(context)) {
            return LocationResult.Error("Location permission not granted")
        }

        return withContext(Dispatchers.IO) {
            try {
                var location: Location? = null
                val highAccLoc = kotlinx.coroutines.withTimeoutOrNull(6000L) {
                    try {
                        val cancelToken = CancellationTokenSource()
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            cancelToken.token
                        ).await()
                    } catch (_: Exception) {
                        null
                    }
                }
                location = highAccLoc ?: runCatching { fusedLocationClient.lastLocation.await() }.getOrNull()

                if (location != null) {
                    val address = geocodeLocation(context, location.latitude, location.longitude)
                    LocationResult.Success(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = address
                    )
                } else {
                    LocationResult.Error("Unable to get GPS location. Please turn on Location services.")
                }
            } catch (e: Exception) {
                LocationResult.Error(e.localizedMessage ?: "Failed to get current location")
            }
        }
    }

    private fun geocodeLocation(context: Context, latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val subLocality = addr.subLocality ?: addr.thoroughfare ?: addr.subAdminArea
                val locality = addr.locality ?: addr.adminArea
                val state = addr.adminArea

                val parts = listOfNotNull(subLocality, locality, state).distinct()
                if (parts.isNotEmpty()) parts.joinToString(", ") else "Lat: ${String.format("%.4f", latitude)}, Lon: ${String.format("%.4f", longitude)}"
            } else {
                "Lat: ${String.format("%.4f", latitude)}, Lon: ${String.format("%.4f", longitude)}"
            }
        } catch (e: Exception) {
            "Lat: ${String.format("%.4f", latitude)}, Lon: ${String.format("%.4f", longitude)}"
        }
    }
}

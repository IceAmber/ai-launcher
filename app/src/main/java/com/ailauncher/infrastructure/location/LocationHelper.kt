package com.ailauncher.infrastructure.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) return null

        return suspendCancellableCoroutine { continuation ->
            try {
                // 先尝试获取最后已知位置
                val gpsProvider = LocationManager.GPS_PROVIDER
                val networkProvider = LocationManager.NETWORK_PROVIDER

                for (provider in listOf(gpsProvider, networkProvider)) {
                    try {
                        val lastLocation = locationManager.getLastKnownLocation(provider)
                        if (lastLocation != null && continuation.isActive) {
                            continuation.resume(lastLocation)
                            return@suspendCancellableCoroutine
                        }
                    } catch (e: SecurityException) {
                        // ignore
                    }
                }

                // 请求位置更新
                val listener = object : LocationListener {
                    var resumed = false
                    override fun onLocationChanged(location: Location) {
                        if (!resumed && continuation.isActive) {
                            resumed = true
                            continuation.resume(location)
                            try { locationManager.removeUpdates(this) } catch (_: Exception) {}
                        }
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                for (provider in listOf(gpsProvider, networkProvider)) {
                    try {
                        if (locationManager.isProviderEnabled(provider)) {
                            locationManager.requestLocationUpdates(provider, 0L, 0f, listener)
                        }
                    } catch (e: SecurityException) {
                        // ignore
                    }
                }

                continuation.invokeOnCancellation {
                    try { locationManager.removeUpdates(listener) } catch (_: Exception) {}
                }

            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    suspend fun getLocationName(location: Location): String? {
        return try {
            val geocoder = Geocoder(context)
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ 使用异步 API
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(location.latitude, location.longitude, 1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<android.location.Address>) {
                                if (cont.isActive) cont.resume(addresses)
                            }
                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(emptyList())
                            }
                        }
                    )
                }
            } else {
                geocoder.getFromLocation(location.latitude, location.longitude, 1)
            }
            addresses?.firstOrNull()?.let { address ->
                buildString {
                    address.locality?.let { append(it) }
                    if (address.adminArea != null && address.adminArea != address.locality) {
                        if (isNotEmpty()) append(", ")
                        append(address.adminArea)
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

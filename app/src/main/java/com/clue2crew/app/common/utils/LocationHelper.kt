package com.clue2crew.app.common.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(onLocationReceived: (Location?) -> Unit) {
        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Log.d("LocationHelper", "Single GPS fix retrieved: lat=${location.latitude}, lon=${location.longitude}, acc=${location.accuracy}m")
                    onLocationReceived(location)
                } else {
                    Log.d("LocationHelper", "Single GPS fix is null, falling back to lastLocation")
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        onLocationReceived(lastLoc)
                    }
                }
            }.addOnFailureListener { e ->
                Log.e("LocationHelper", "Error getting single location", e)
                onLocationReceived(null)
            }
        } catch (e: Exception) {
            Log.e("LocationHelper", "Exception in getCurrentLocation", e)
            onLocationReceived(null)
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onLocationUpdate: (Location) -> Unit) {
        // Try to get last known location immediately
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                Log.d("LocationHelper", "Immediate lastLocation fix: lat=${it.latitude}, lon=${it.longitude}, acc=${it.accuracy}m")
                onLocationUpdate(it)
            }
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdateDelayMillis(3000)
            .setMinUpdateDistanceMeters(0f)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    Log.d("LocationHelper", "Live GPS update: lat=${location.latitude}, lon=${location.longitude}, acc=${location.accuracy}m")
                    onLocationUpdate(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            Log.d("LocationHelper", "Continuous high-accuracy location updates requested (2000ms interval)")
        } catch (e: Exception) {
            Log.e("LocationHelper", "Error starting location updates", e)
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            Log.d("LocationHelper", "Location updates stopped")
        }
        locationCallback = null
    }
}

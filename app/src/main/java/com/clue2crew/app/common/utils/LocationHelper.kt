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
                    Log.d("LocationHelper", "Location retrieved: ${location.latitude}, ${location.longitude}")
                } else {
                    Log.d("LocationHelper", "Location is null, trying last location")
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        onLocationReceived(lastLoc)
                    }
                    return@addOnSuccessListener
                }
                onLocationReceived(location)
            }.addOnFailureListener { e ->
                Log.e("LocationHelper", "Error getting location", e)
                onLocationReceived(null)
            }
        } catch (e: Exception) {
            Log.e("LocationHelper", "Exception in getCurrentLocation", e)
            onLocationReceived(null)
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onLocationUpdate: (Location) -> Unit) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(3000)
            .setMaxUpdateDelayMillis(10000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { 
                    Log.d("LocationHelper", "Periodic update: ${it.latitude}, ${it.longitude}")
                    onLocationUpdate(it) 
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e("LocationHelper", "Error starting location updates", e)
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }
}

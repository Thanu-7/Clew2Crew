package com.clue2crew.app.common.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class LocationHelper(context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

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
                    Log.d("LocationHelper", "Location is null")
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
}

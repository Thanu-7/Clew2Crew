package com.clue2crew.app.common.utils

import android.location.Location
import kotlin.math.*

object LocationUtils {

    /**
     * Calculates the exact distance between two points in meters using WGS84 ellipsoid algorithm
     * with clamped Haversine formula as fallback.
     */
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        return try {
            Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            results[0].toDouble()
        } catch (e: Exception) {
            val r = 6371e3 // Earth's radius in meters
            val phi1 = Math.toRadians(lat1)
            val phi2 = Math.toRadians(lat2)
            val deltaPhi = Math.toRadians(lat2 - lat1)
            val deltaLambda = Math.toRadians(lon2 - lon1)

            val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                    cos(phi1) * cos(phi2) *
                    sin(deltaLambda / 2) * sin(deltaLambda / 2)
            val aClamped = a.coerceIn(0.0, 1.0)
            val c = 2 * atan2(sqrt(aClamped), sqrt(1.0 - aClamped))

            r * c
        }
    }

    /**
     * Reunited threshold check (distance <= 10.0 meters).
     */
    fun isReunited(distanceMeters: Double): Boolean {
        return distanceMeters <= 10.0
    }

    /**
     * Calculates the distance between two points.
     * Returns a formatted string (e.g., "25 m" or "1.2 km").
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
        val distance = calculateDistanceMeters(lat1, lon1, lat2, lon2)
        return if (distance < 1000) {
            "${distance.roundToInt()} m"
        } else {
            "${String.format("%.1f", distance / 1000)} km"
        }
    }

    /**
     * Calculates the initial bearing (angle) from point 1 to point 2 using WGS84.
     * Returns degrees from 0 to 360.
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(2)
        return try {
            Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            (results[1] + 360) % 360
        } catch (e: Exception) {
            val phi1 = Math.toRadians(lat1)
            val phi2 = Math.toRadians(lat2)
            val deltaLambda = Math.toRadians(lon2 - lon1)

            val y = sin(deltaLambda) * cos(phi2)
            val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
            
            val bearing = Math.toDegrees(atan2(y, x))
            ((bearing + 360) % 360).toFloat()
        }
    }

    /**
     * Converts a bearing degree into a short 8-cardinal direction string (N, NE, E, SE, S, SW, W, NW).
     */
    fun getCardinalDirection(bearing: Float): String {
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((bearing + 22.5) % 360 / 45).toInt() % 8
        return directions[index]
    }

    /**
     * Converts a bearing degree into a full cardinal direction string (North, Northeast, etc.).
     */
    fun getFullCardinalDirection(bearing: Float): String {
        val directions = arrayOf("North", "Northeast", "East", "Southeast", "South", "Southwest", "West", "Northwest")
        val index = ((bearing + 22.5) % 360 / 45).toInt() % 8
        return directions[index]
    }
}

package com.clue2crew.app.common.utils

import kotlin.math.*

object LocationUtils {

    /**
     * Calculates the exact distance between two points in meters using the Haversine formula.
     */
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // Earth's radius in meters
        val phi1 = lat1 * PI / 180
        val phi2 = lat2 * PI / 180
        val deltaPhi = (lat2 - lat1) * PI / 180
        val deltaLambda = (lon2 - lon1) * PI / 180

        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }

    /**
     * Reunited threshold check (distance <= 10.0 meters).
     */
    fun isReunited(distanceMeters: Double): Boolean {
        return distanceMeters <= 10.0
    }

    /**
     * Calculates the distance between two points using the Haversine formula.
     * Returns a formatted string (e.g., "250 m" or "1.2 km").
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
     * Calculates the bearing (angle) from point 1 to point 2.
     * Returns degrees from 0 to 360.
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }

    /**
     * Converts a bearing degree into a short 8-cardinal direction string (N, NE, E, SE, S, SW, W, NW).
     */
    fun getCardinalDirection(bearing: Float): String {
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((bearing + 22.5) % 360 / 45).toInt()
        return directions[index]
    }

    /**
     * Converts a bearing degree into a full cardinal direction string (North, Northeast, etc.).
     */
    fun getFullCardinalDirection(bearing: Float): String {
        val directions = arrayOf("North", "Northeast", "East", "Southeast", "South", "Southwest", "West", "Northwest")
        val index = ((bearing + 22.5) % 360 / 45).toInt()
        return directions[index]
    }
}

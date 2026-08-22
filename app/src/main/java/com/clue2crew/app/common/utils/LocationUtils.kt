package com.clue2crew.app.common.utils

import kotlin.math.*

object LocationUtils {

    /**
     * Calculates the distance between two points using the Haversine formula.
     * Returns a formatted string (e.g., "250 m" or "1.2 km").
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
        val r = 6371e3 // Earth's radius in meters
        val phi1 = lat1 * PI / 180
        val phi2 = lat2 * PI / 180
        val deltaPhi = (lat2 - lat1) * PI / 180
        val deltaLambda = (lon2 - lon1) * PI / 180

        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        val distance = r * c // in meters

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
     * Converts a bearing degree into a cardinal direction string.
     */
    fun getCardinalDirection(bearing: Float): String {
        val directions = arrayOf("NORTH", "NORTH-EAST", "EAST", "SOUTH-EAST", "SOUTH", "SOUTH-WEST", "WEST", "NORTH-WEST")
        val index = ((bearing + 22.5) % 360 / 45).toInt()
        return directions[index]
    }
}

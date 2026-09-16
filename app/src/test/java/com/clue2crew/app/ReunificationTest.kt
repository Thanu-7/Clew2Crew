package com.clue2crew.app

import com.clue2crew.app.common.utils.LocationUtils
import com.clue2crew.app.data.security.AuthProtocol
import com.clue2crew.app.data.security.KeyStoreManager
import org.junit.Assert.*
import org.junit.Test

class ReunificationTest {

    @Test
    fun testLocationPacketSerializationAndDeserialization() {
        val familyKey = KeyStoreManager.deriveFamilySharedKey("AB3DX9")
        val cClient = AuthProtocol.generateNonce()
        val cServer = AuthProtocol.generateNonce()
        val sessionKey = KeyStoreManager.deriveSessionKey(familyKey, cClient, cServer)

        val memberId = "member_alice_123"
        val lat = 12.971598
        val lon = 77.594562
        val accuracy = 4.5f
        val timestamp = System.currentTimeMillis()

        // Create packet
        val packet = AuthProtocol.createLocationPacket(sessionKey, memberId, lat, lon, accuracy, timestamp)
        assertNotNull(packet)
        assertTrue(packet.isNotEmpty())

        // Parse packet
        val locationData = AuthProtocol.parseLocationPacket(packet, sessionKey, timestamp)
        assertEquals(memberId, locationData.memberId)
        assertEquals(lat, locationData.latitude, 0.000001)
        assertEquals(lon, locationData.longitude, 0.000001)
        assertEquals(accuracy, locationData.accuracy, 0.1f)
        assertEquals(timestamp, locationData.timestampMs)
    }

    @Test
    fun testHaversineDistanceCalculation() {
        // Points ~111 meters apart vertically on equator
        val lat1 = 0.0
        val lon1 = 0.0
        val lat2 = 0.001 // ~111 meters
        val lon2 = 0.0

        val distanceMeters = LocationUtils.calculateDistanceMeters(lat1, lon1, lat2, lon2)
        assertTrue("Distance should be approx 111m, got $distanceMeters", distanceMeters in 110.0..112.0)

        val formattedDistance = LocationUtils.calculateDistance(lat1, lon1, lat2, lon2)
        assertEquals("111 m", formattedDistance)
    }

    @Test
    fun testReunitedThreshold() {
        val closeDistance = 8.5 // meters
        val farDistance = 15.2 // meters

        assertTrue(LocationUtils.isReunited(closeDistance))
        assertFalse(LocationUtils.isReunited(farDistance))
        assertTrue(LocationUtils.isReunited(10.0))
    }

    @Test
    fun testCardinalDirectionConversion() {
        // True North
        assertEquals("N", LocationUtils.getCardinalDirection(0f))
        assertEquals("N", LocationUtils.getCardinalDirection(350f))

        // Northeast
        assertEquals("NE", LocationUtils.getCardinalDirection(45f))

        // East
        assertEquals("E", LocationUtils.getCardinalDirection(90f))

        // Southeast
        assertEquals("SE", LocationUtils.getCardinalDirection(135f))

        // South
        assertEquals("S", LocationUtils.getCardinalDirection(180f))

        // Southwest
        assertEquals("SW", LocationUtils.getCardinalDirection(225f))

        // West
        assertEquals("W", LocationUtils.getCardinalDirection(270f))

        // Northwest
        assertEquals("NW", LocationUtils.getCardinalDirection(315f))
    }
}

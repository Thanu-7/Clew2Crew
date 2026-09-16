package com.clue2crew.app.data.security

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.SecretKey

object AuthProtocol {

    const val MAX_TIMESTAMP_DELTA_MS = 300_000L // 5 minutes freshness window
    private val secureRandom = SecureRandom()

    fun generateNonce(): ByteArray {
        val nonce = ByteArray(16)
        secureRandom.nextBytes(nonce)
        return nonce
    }

    fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }

    fun String.hexToByteArray(): ByteArray {
        val len = length
        require(len % 2 == 0) { "Hex string length must be even" }
        val result = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            val first = Character.digit(this[i], 16)
            val second = Character.digit(this[i + 1], 16)
            require(first != -1 && second != -1) { "Invalid hex digit in $this" }
            result[i / 2] = ((first shl 4) + second).toByte()
        }
        return result
    }

    // Step 1: Unauthenticated Init message
    fun createAuthInit(cClient: ByteArray, memberId: String): String {
        return "AUTH_INIT:${cClient.toHex()}:$memberId"
    }

    fun parseAuthInit(initMsg: String): Pair<ByteArray, String> {
        val parts = initMsg.split(":")
        require(parts.size == 3 && parts[0] == "AUTH_INIT") { "Invalid AUTH_INIT format" }
        return Pair(parts[1].hexToByteArray(), parts[2])
    }

    // Step 2: Server Proof & Challenge
    fun createServerProof(
        sessionKey: SecretKey,
        serverMemberId: String,
        cClientHex: String,
        familyName: String,
        familyId: String,
        timestampMs: Long = System.currentTimeMillis()
    ): ByteArray {
        val payload = "AUTH_RESP:$serverMemberId:$cClientHex:$timestampMs:$familyName:$familyId"
        return CryptoManager.encrypt(payload.toByteArray(StandardCharsets.UTF_8), sessionKey)
    }

    fun parseAndVerifyServerProof(
        encryptedProof: ByteArray,
        sessionKey: SecretKey,
        expectedCClientHex: String,
        nowMs: Long = System.currentTimeMillis()
    ): ServerAuthResult {
        val decryptedBytes = CryptoManager.decrypt(encryptedProof, sessionKey)
        val decryptedStr = String(decryptedBytes, StandardCharsets.UTF_8)
        val parts = decryptedStr.split(":")
        require(parts.size >= 4 && parts[0] == "AUTH_RESP") { "Invalid AUTH_RESP format" }

        val serverMemberId = parts[1]
        val cClientHex = parts[2]
        val timestampMs = parts[3].toLongOrNull() ?: throw IllegalArgumentException("Invalid timestamp")
        
        val familyName = if (parts.size > 4) parts[4] else "Unknown Family"
        val familyId = if (parts.size > 5) parts[5] else ""

        require(cClientHex == expectedCClientHex) { "Challenge mismatch" }
        require(Math.abs(nowMs - timestampMs) <= MAX_TIMESTAMP_DELTA_MS) { "Stale timestamp" }

        return ServerAuthResult(serverMemberId, familyName, familyId)
    }

    data class ServerAuthResult(val memberId: String, val familyName: String, val familyId: String)

    // Step 3: Client Proof
    fun createClientProof(
        sessionKey: SecretKey,
        clientMemberId: String,
        cServerHex: String,
        timestampMs: Long = System.currentTimeMillis()
    ): ByteArray {
        val payload = "AUTH_CONFIRM:$clientMemberId:$cServerHex:$timestampMs"
        return CryptoManager.encrypt(payload.toByteArray(StandardCharsets.UTF_8), sessionKey)
    }

    fun parseAndVerifyClientProof(
        encryptedProof: ByteArray,
        sessionKey: SecretKey,
        expectedCServerHex: String,
        nowMs: Long = System.currentTimeMillis()
    ): String {
        val decryptedBytes = CryptoManager.decrypt(encryptedProof, sessionKey)
        val decryptedStr = String(decryptedBytes, StandardCharsets.UTF_8)
        val parts = decryptedStr.split(":")
        require(parts.size == 4 && parts[0] == "AUTH_CONFIRM") { "Invalid AUTH_CONFIRM format" }

        val clientMemberId = parts[1]
        val cServerHex = parts[2]
        val timestampMs = parts[3].toLongOrNull() ?: throw IllegalArgumentException("Invalid timestamp")

        require(cServerHex == expectedCServerHex) { "Challenge mismatch: expected $expectedCServerHex, got $cServerHex" }
        require(Math.abs(nowMs - timestampMs) <= MAX_TIMESTAMP_DELTA_MS) { "Stale timestamp: delta=${Math.abs(nowMs - timestampMs)} ms" }

        return clientMemberId
    }

    // Step 4: Encrypted Location Exchange
    fun createLocationPacket(
        sessionKey: SecretKey,
        memberId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float = 0f,
        timestampMs: Long = System.currentTimeMillis()
    ): ByteArray {
        val payload = "LOC:$memberId:$latitude:$longitude:$accuracy:$timestampMs"
        return CryptoManager.encrypt(payload.toByteArray(StandardCharsets.UTF_8), sessionKey)
    }

    fun parseLocationPacket(
        encryptedPacket: ByteArray,
        sessionKey: SecretKey,
        nowMs: Long = System.currentTimeMillis()
    ): LocationData {
        val decryptedBytes = CryptoManager.decrypt(encryptedPacket, sessionKey)
        val decryptedStr = String(decryptedBytes, StandardCharsets.UTF_8)
        val parts = decryptedStr.split(":")
        require(parts.size >= 5 && parts[0] == "LOC") { "Invalid Location Packet format" }

        val memberId = parts[1]
        val lat = parts[2].toDouble()
        val lon = parts[3].toDouble()
        val accuracy = if (parts.size >= 6) parts[4].toFloat() else 0f
        val timestampMs = if (parts.size >= 6) parts[5].toLong() else parts[4].toLong()

        require(Math.abs(nowMs - timestampMs) <= MAX_TIMESTAMP_DELTA_MS) { "Stale location packet" }

        return LocationData(memberId, lat, lon, accuracy, timestampMs)
    }

    data class LocationData(
        val memberId: String,
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float = 0f,
        val timestampMs: Long
    )
}

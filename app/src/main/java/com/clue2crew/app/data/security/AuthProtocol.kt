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
    fun createAuthInit(cClient: ByteArray, memberId: String, name: String): String {
        return "AUTH_INIT:${cClient.toHex()}:$memberId:$name"
    }

    fun parseAuthInit(initMsg: String): Triple<ByteArray, String, String> {
        val parts = initMsg.split(":")
        require(parts.size >= 4 && parts[0] == "AUTH_INIT") { "Invalid AUTH_INIT format" }
        return Triple(parts[1].hexToByteArray(), parts[2], parts[3])
    }

    // Step 2: Server Proof & Challenge
    fun createServerProof(
        sessionKey: SecretKey,
        serverMemberId: String,
        cClientHex: String,
        familyName: String,
        familyId: String,
        serverName: String,
        timestampMs: Long = System.currentTimeMillis()
    ): ByteArray {
        val payload = "AUTH_RESP:$serverMemberId:$cClientHex:$timestampMs:$familyName:$familyId:$serverName"
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
        val serverName = if (parts.size > 6) parts[6] else "Family Member"

        require(cClientHex == expectedCClientHex) { "Challenge mismatch" }
        require(Math.abs(nowMs - timestampMs) <= MAX_TIMESTAMP_DELTA_MS) { "Stale timestamp" }

        return ServerAuthResult(serverMemberId, familyName, familyId, serverName)
    }

    data class ServerAuthResult(
        val memberId: String,
        val familyName: String,
        val familyId: String,
        val memberName: String
    )

    // Step 3: Client Proof
    fun createClientProof(
        sessionKey: SecretKey,
        clientMemberId: String,
        clientName: String,
        cServerHex: String,
        timestampMs: Long = System.currentTimeMillis()
    ): ByteArray {
        val payload = "AUTH_CONFIRM:$clientMemberId:$clientName:$cServerHex:$timestampMs"
        return CryptoManager.encrypt(payload.toByteArray(StandardCharsets.UTF_8), sessionKey)
    }

    fun parseAndVerifyClientProof(
        encryptedProof: ByteArray,
        sessionKey: SecretKey,
        expectedCServerHex: String,
        nowMs: Long = System.currentTimeMillis()
    ): Pair<String, String> {
        val decryptedBytes = CryptoManager.decrypt(encryptedProof, sessionKey)
        val decryptedStr = String(decryptedBytes, StandardCharsets.UTF_8)
        val parts = decryptedStr.split(":")
        require(parts.size >= 5 && parts[0] == "AUTH_CONFIRM") { "Invalid AUTH_CONFIRM format" }

        val clientMemberId = parts[1]
        val clientName = parts[2]
        val cServerHex = parts[3]
        val timestampMs = parts[4].toLongOrNull() ?: throw IllegalArgumentException("Invalid timestamp")

        require(cServerHex == expectedCServerHex) { "Challenge mismatch" }
        require(Math.abs(nowMs - timestampMs) <= MAX_TIMESTAMP_DELTA_MS) { "Stale timestamp" }

        return Pair(clientMemberId, clientName)
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

    // Step 5: Encrypted Offline Messaging
    fun createMessagePacket(
        sessionKey: SecretKey,
        senderId: String,
        messageText: String,
        timestampMs: Long = System.currentTimeMillis()
    ): ByteArray {
        val payload = "MSG:$senderId:$timestampMs:$messageText"
        return CryptoManager.encrypt(payload.toByteArray(StandardCharsets.UTF_8), sessionKey)
    }

    fun parseMessagePacket(
        encryptedPacket: ByteArray,
        sessionKey: SecretKey,
        nowMs: Long = System.currentTimeMillis()
    ): MessageData {
        val decryptedBytes = CryptoManager.decrypt(encryptedPacket, sessionKey)
        val decryptedStr = String(decryptedBytes, StandardCharsets.UTF_8)
        val parts = decryptedStr.split(":", limit = 4)
        require(parts.size == 4 && parts[0] == "MSG") { "Invalid Message Packet format" }

        val senderId = parts[1]
        val timestampMs = parts[2].toLong()
        val text = parts[3]

        // Allow messages even if slightly older than 5 mins if we want persistence, 
        // but let's keep the delta check for real-time security
        require(Math.abs(nowMs - timestampMs) <= MAX_TIMESTAMP_DELTA_MS) { "Stale message packet" }

        return MessageData(senderId, text, timestampMs)
    }

    data class MessageData(
        val senderId: String,
        val messageText: String,
        val timestampMs: Long
    )
}

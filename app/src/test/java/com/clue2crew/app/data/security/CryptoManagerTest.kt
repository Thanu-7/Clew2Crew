package com.clue2crew.app.data.security

import com.clue2crew.app.data.security.AuthProtocol.toHex
import org.junit.Assert.*
import org.junit.Test
import javax.crypto.AEADBadTagException

class CryptoManagerTest {

    private val samplePairingCode = "AB3DX9"
    private val sampleMemberIdClient = "11111111-2222-3333-4444-555555555555"
    private val sampleMemberIdServer = "66666666-7777-8888-9999-000000000000"

    @Test
    fun testEncryptionDecryptionRoundTrip() {
        val key = KeyStoreManager.deriveFamilySharedKey(samplePairingCode)
        val plaintext = "Hello Clew2Crew Security!".toByteArray(Charsets.UTF_8)

        val encryptedPacket = CryptoManager.encrypt(plaintext, key)
        val decryptedBytes = CryptoManager.decrypt(encryptedPacket, key)

        assertArrayEquals(plaintext, decryptedBytes)
    }

    @Test
    fun testNonceUniqueness() {
        val key = KeyStoreManager.deriveFamilySharedKey(samplePairingCode)
        val plaintext = "Same Plaintext".toByteArray(Charsets.UTF_8)

        val ivs = mutableSetOf<String>()
        for (i in 1..100) {
            val packet = CryptoManager.encrypt(plaintext, key)
            // IV is at offset 2..13 (12 bytes)
            val ivHex = packet.sliceArray(2..13).toHex()
            assertFalse("Duplicate IV found!", ivs.contains(ivHex))
            ivs.add(ivHex)
        }
        assertEquals(100, ivs.size)
    }

    @Test(expected = AEADBadTagException::class)
    fun testTamperedCiphertextRejection() {
        val key = KeyStoreManager.deriveFamilySharedKey(samplePairingCode)
        val plaintext = "Secret Message".toByteArray(Charsets.UTF_8)

        val packet = CryptoManager.encrypt(plaintext, key)
        // Flip a byte in the ciphertext payload
        packet[packet.size - 1] = (packet[packet.size - 1].toInt() xor 0xFF).toByte()

        CryptoManager.decrypt(packet, key)
    }

    @Test(expected = AEADBadTagException::class)
    fun testWrongKeyRejection() {
        val keyCorrect = KeyStoreManager.deriveFamilySharedKey("AB3DX9")
        val keyWrong = KeyStoreManager.deriveFamilySharedKey("WRONG1")
        val plaintext = "Secret Data".toByteArray(Charsets.UTF_8)

        val packet = CryptoManager.encrypt(plaintext, keyCorrect)

        CryptoManager.decrypt(packet, keyWrong)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testMalformedPacketRejection() {
        val key = KeyStoreManager.deriveFamilySharedKey(samplePairingCode)
        val malformedBytes = byteArrayOf(0x01, 0x0C, 0x01, 0x02) // Too short

        CryptoManager.decrypt(malformedBytes, key)
    }

    @Test
    fun testKeyGenerationAndDerivation() {
        val familyKey1 = KeyStoreManager.deriveFamilySharedKey("AB3DX9")
        val familyKey2 = KeyStoreManager.deriveFamilySharedKey("AB3DX9")
        val familyKeyDifferent = KeyStoreManager.deriveFamilySharedKey("XYZ999")

        assertArrayEquals(familyKey1.encoded, familyKey2.encoded)
        assertFalse(familyKey1.encoded.contentEquals(familyKeyDifferent.encoded))

        val masterKey = KeyStoreManager.getOrCreateMasterKey()
        assertNotNull(masterKey)
    }

    @Test
    fun testSessionKeyUniquenessForDifferentChallenges() {
        val familyKey = KeyStoreManager.deriveFamilySharedKey(samplePairingCode)
        val cClient1 = AuthProtocol.generateNonce()
        val cServer1 = AuthProtocol.generateNonce()

        val cClient2 = AuthProtocol.generateNonce()
        val cServer2 = AuthProtocol.generateNonce()

        val sessionKey1 = KeyStoreManager.deriveSessionKey(familyKey, cClient1, cServer1)
        val sessionKey2 = KeyStoreManager.deriveSessionKey(familyKey, cClient2, cServer2)

        assertFalse(sessionKey1.encoded.contentEquals(sessionKey2.encoded))
    }

    @Test
    fun testFullSuccessfulAuthenticationFlow() {
        val pairingCode = "TEST99"
        val familyKeyClient = KeyStoreManager.deriveFamilySharedKey(pairingCode)
        val familyKeyServer = KeyStoreManager.deriveFamilySharedKey(pairingCode)

        // Step 1: Client -> Server Init
        val cClient = AuthProtocol.generateNonce()
        val initMsg = AuthProtocol.createAuthInit(cClient, sampleMemberIdClient)
        val (parsedCClient, parsedClientId) = AuthProtocol.parseAuthInit(initMsg)

        assertEquals(sampleMemberIdClient, parsedClientId)
        assertArrayEquals(cClient, parsedCClient)

        // Step 2: Server -> Client Challenge
        val cServer = AuthProtocol.generateNonce()
        val serverSessionKey = KeyStoreManager.deriveSessionKey(familyKeyServer, parsedCClient, cServer)
        val serverProof = AuthProtocol.createServerProof(
            serverSessionKey,
            sampleMemberIdServer,
            parsedCClient.toHex()
        )

        // Client receives challenge & verifies server
        val clientSessionKey = KeyStoreManager.deriveSessionKey(familyKeyClient, cClient, cServer)
        val verifiedServerMemberId = AuthProtocol.parseAndVerifyServerProof(
            serverProof,
            clientSessionKey,
            cClient.toHex()
        )
        assertEquals(sampleMemberIdServer, verifiedServerMemberId)

        // Step 3: Client -> Server Proof
        val clientProof = AuthProtocol.createClientProof(
            clientSessionKey,
            sampleMemberIdClient,
            cServer.toHex()
        )

        // Server verifies client proof
        val verifiedClientMemberId = AuthProtocol.parseAndVerifyClientProof(
            clientProof,
            serverSessionKey,
            cServer.toHex()
        )
        assertEquals(sampleMemberIdClient, verifiedClientMemberId)
    }

    @Test(expected = AEADBadTagException::class)
    fun testAuthenticationFailureWrongPairingCode() {
        val familyKeyClient = KeyStoreManager.deriveFamilySharedKey("RIGHT1")
        val familyKeyServer = KeyStoreManager.deriveFamilySharedKey("WRONG2")

        val cClient = AuthProtocol.generateNonce()
        val cServer = AuthProtocol.generateNonce()

        val serverSessionKey = KeyStoreManager.deriveSessionKey(familyKeyServer, cClient, cServer)
        val serverProof = AuthProtocol.createServerProof(serverSessionKey, sampleMemberIdServer, cClient.toHex())

        val clientSessionKey = KeyStoreManager.deriveSessionKey(familyKeyClient, cClient, cServer)
        // Decryption fails with wrong key
        AuthProtocol.parseAndVerifyServerProof(serverProof, clientSessionKey, cClient.toHex())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testStaleTimestampReplayRejection() {
        val familyKey = KeyStoreManager.deriveFamilySharedKey(samplePairingCode)
        val cClient = AuthProtocol.generateNonce()
        val cServer = AuthProtocol.generateNonce()
        val sessionKey = KeyStoreManager.deriveSessionKey(familyKey, cClient, cServer)

        val staleTimestamp = System.currentTimeMillis() - 600_000L // 10 minutes ago
        val staleProof = AuthProtocol.createServerProof(
            sessionKey,
            sampleMemberIdServer,
            cClient.toHex(),
            timestampMs = staleTimestamp
        )

        // Attempt verification with current time -> Rejects stale timestamp (> 5 mins)
        AuthProtocol.parseAndVerifyServerProof(
            staleProof,
            sessionKey,
            cClient.toHex(),
            nowMs = System.currentTimeMillis()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testChallengeReuseRejection() {
        val familyKey = KeyStoreManager.deriveFamilySharedKey(samplePairingCode)
        val cClientOriginal = AuthProtocol.generateNonce()
        val cClientReused = AuthProtocol.generateNonce()
        val cServer = AuthProtocol.generateNonce()

        val sessionKey = KeyStoreManager.deriveSessionKey(familyKey, cClientOriginal, cServer)
        val serverProof = AuthProtocol.createServerProof(
            sessionKey,
            sampleMemberIdServer,
            cClientOriginal.toHex()
        )

        // Verifying server proof expecting cClientReused fails challenge check
        AuthProtocol.parseAndVerifyServerProof(
            serverProof,
            sessionKey,
            expectedCClientHex = cClientReused.toHex()
        )
    }

    @Test(expected = AEADBadTagException::class)
    fun testFabricatedResponseRejection() {
        val familyKey = KeyStoreManager.deriveFamilySharedKey(samplePairingCode)
        val cClient = AuthProtocol.generateNonce()
        val cServer = AuthProtocol.generateNonce()
        val sessionKey = KeyStoreManager.deriveSessionKey(familyKey, cClient, cServer)

        val fakeKey = KeyStoreManager.deriveFamilySharedKey("FAKE00")
        val fakeSessionKey = KeyStoreManager.deriveSessionKey(fakeKey, cClient, cServer)
        val fabricatedProof = AuthProtocol.createServerProof(
            fakeSessionKey,
            sampleMemberIdServer,
            cClient.toHex()
        )

        // Attempting to decrypt fabricated proof with real sessionKey throws AEADBadTagException
        AuthProtocol.parseAndVerifyServerProof(
            fabricatedProof,
            sessionKey,
            expectedCClientHex = cClient.toHex()
        )
    }
}

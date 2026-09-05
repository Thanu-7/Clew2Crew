package com.clue2crew.app.data.security

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoManager {

    private const val PROTOCOL_VERSION: Byte = 0x01
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val TRANSFORM = "AES/GCM/NoPadding"

    private val secureRandom = SecureRandom()

    /**
     * Encrypts plaintext using AES-256-GCM with a fresh 12-byte random IV.
     * Binary Format: [1B Version] [1B IV_Len] [12B IV] [2B Payload_Len] [N Bytes Ciphertext + 16B GCM Tag]
     */
    fun encrypt(plaintext: ByteArray, secretKey: SecretKey): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORM)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

        val ciphertext = cipher.doFinal(plaintext)

        val buffer = ByteBuffer.allocate(1 + 1 + GCM_IV_LENGTH + 2 + ciphertext.size)
        buffer.put(PROTOCOL_VERSION)
        buffer.put(GCM_IV_LENGTH.toByte())
        buffer.put(iv)
        buffer.putShort(ciphertext.size.toShort())
        buffer.put(ciphertext)

        return buffer.array()
    }

    /**
     * Decrypts binary packet using AES-256-GCM.
     * Validates protocol version, IV length, and GCM authentication tag.
     * Throws AEADBadTagException if tampered or wrong key.
     * Throws IllegalArgumentException if packet structure is invalid.
     */
    fun decrypt(packedBytes: ByteArray, secretKey: SecretKey): ByteArray {
        if (packedBytes.size < (1 + 1 + GCM_IV_LENGTH + 2 + 16)) {
            throw IllegalArgumentException("Malformed packet: Size too small (${packedBytes.size} bytes)")
        }

        val buffer = ByteBuffer.wrap(packedBytes)
        val version = buffer.get()
        if (version != PROTOCOL_VERSION) {
            throw IllegalArgumentException("Unsupported protocol version: $version")
        }

        val ivLen = buffer.get().toInt() and 0xFF
        if (ivLen != GCM_IV_LENGTH) {
            throw IllegalArgumentException("Invalid IV length: $ivLen")
        }

        val iv = ByteArray(GCM_IV_LENGTH)
        buffer.get(iv)

        val payloadLen = buffer.short.toInt() and 0xFFFF
        val remaining = buffer.remaining()
        if (payloadLen != remaining) {
            throw IllegalArgumentException("Payload length mismatch: Header specifies $payloadLen but remaining is $remaining")
        }

        val ciphertext = ByteArray(payloadLen)
        buffer.get(ciphertext)

        val cipher = Cipher.getInstance(TRANSFORM)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

        return cipher.doFinal(ciphertext)
    }
}

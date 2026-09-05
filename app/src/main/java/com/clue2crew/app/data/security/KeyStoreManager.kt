package com.clue2crew.app.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object KeyStoreManager {

    private const val MASTER_KEY_ALIAS = "Clew2CrewMasterKey"
    private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"

    private const val FAMILY_SALT = "Clew2CrewFamilySalt_v1"
    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_SIZE_BITS = 256

    private val SESSION_INFO = "Clew2Crew-BLE-Session-v1".toByteArray(StandardCharsets.UTF_8)

    /**
     * Gets or creates the 256-bit AES master key in AndroidKeyStore.
     * Fallbacks to standard JVM KeyGenerator when running in host unit test environments.
     */
    fun getOrCreateMasterKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply { load(null) }
            if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE_PROVIDER
                )
                val spec = KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
            keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
        } catch (e: Exception) {
            // Fallback for Host JVM Unit Tests where AndroidKeyStore provider is absent
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(KEY_SIZE_BITS, SecureRandom())
            keyGen.generateKey()
        }
    }

    /**
     * Derives the FamilySharedKey (256-bit AES) from pairingCode using PBKDF2-HMAC-SHA256.
     */
    fun deriveFamilySharedKey(pairingCode: String): SecretKey {
        val cleanCode = pairingCode.trim().uppercase()
        val spec = PBEKeySpec(
            cleanCode.toCharArray(),
            FAMILY_SALT.toByteArray(StandardCharsets.UTF_8),
            PBKDF2_ITERATIONS,
            KEY_SIZE_BITS
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Derives the 256-bit SessionKey from FamilySharedKey, C_client, and C_server using HKDF-SHA256 (RFC 5869).
     * Salt = C_client (16B) || C_server (16B)
     * Info = "Clew2Crew-BLE-Session-v1"
     */
    fun deriveSessionKey(familySharedKey: SecretKey, cClient: ByteArray, cServer: ByteArray): SecretKey {
        val ikm = familySharedKey.encoded
        val salt = cClient + cServer // 32 bytes

        // HKDF-Extract: PRK = HMAC-SHA256(Salt, IKM)
        val macExtract = Mac.getInstance("HmacSHA256")
        val saltKey = SecretKeySpec(salt, "HmacSHA256")
        macExtract.init(saltKey)
        val prk = macExtract.doFinal(ikm)

        // HKDF-Expand: OKM = HMAC-SHA256(PRK, Info || 0x01)
        val macExpand = Mac.getInstance("HmacSHA256")
        val prkKey = SecretKeySpec(prk, "HmacSHA256")
        macExpand.init(prkKey)
        macExpand.update(SESSION_INFO)
        macExpand.update(0x01.toByte())
        val okm = macExpand.doFinal() // 32 bytes (256 bits)

        return SecretKeySpec(okm, "AES")
    }
}

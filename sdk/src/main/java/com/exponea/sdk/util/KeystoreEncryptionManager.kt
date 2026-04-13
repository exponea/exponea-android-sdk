package com.exponea.sdk.util

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import com.exponea.sdk.preferences.ExponeaPreferences
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.util.Calendar
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal

/**
 * Manages AES-256-GCM encryption/decryption backed by the Android Keystore.
 *
 * - API 23+: AES key stored directly in the Keystore.
 * - API 21-22: RSA key pair in the Keystore is used to wrap/unwrap a software-generated AES key
 * that is persisted (encrypted) in SharedPreferences.
 *
 * All public methods return null on failure so callers can fall back to unencrypted storage.
 */
internal class KeystoreEncryptionManager(
    private val context: Context,
    private val prefs: ExponeaPreferences,
    private val keyAlias: String = DEFAULT_KEY_ALIAS
) {
    companion object {
        private const val DEFAULT_KEY_ALIAS = "exponea_auth_token_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val GCM_TAG_LENGTH = 128
        private const val SEPARATOR = ":"
        private const val PREF_WRAPPED_AES_KEY = "exponea_wrapped_aes_key"
        private const val RSA_CIPHER = "RSA/ECB/PKCS1Padding"
    }

    private val rsaKeyAlias = "${keyAlias}_rsa"

    private val keyStore: KeyStore? by lazy {
        try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        } catch (e: Exception) {
            Logger.e(this, "$ANDROID_KEYSTORE unavailable: ${e.message}")
            null
        }
    }

    @Synchronized
    fun encrypt(plaintext: String): String? {
        return try {
            val key = getOrCreateAesKey()
            if (key != null) {
                encryptWith(key, plaintext)
            } else {
                Logger.w(this, "Key unavailable, attempting key regeneration")
                tryRegenerateAndEncrypt(plaintext)
            }
        } catch (e: Exception) {
            Logger.w(this, "Encryption failed, attempting key regeneration: ${e.message}")
            tryRegenerateAndEncrypt(plaintext)
        }
    }

    @Synchronized
    fun decrypt(encoded: String): String? {
        return try {
            val parts = encoded.split(SEPARATOR)

            if (parts.size != 2) {
                Logger.w(this, "Decryption failed: malformed input (expected iv:ciphertext)")
                return null
            }

            val key = getAesKey() ?: run {
                Logger.e(this, "Decryption failed: AES key unavailable")
                return null
            }

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            Logger.e(this, "Decryption failed: ${e.message}")
            null
        }
    }

    @Synchronized
    fun deleteKey() {
        try {
            keyStore?.let {
                it.deleteEntry(keyAlias)
                it.deleteEntry(rsaKeyAlias)
            }
        } catch (e: Exception) {
            Logger.e(this, "Failed to delete Keystore entries: ${e.message}")
        }

        prefs.remove(PREF_WRAPPED_AES_KEY)
    }

    private fun getOrCreateAesKey(): SecretKey? {
        return getAesKey() ?: createAesKey()
    }

    private fun getAesKey(): SecretKey? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getKeystoreAesKey()
        } else {
            unwrapAesKey()
        }
    }

    private fun createAesKey(): SecretKey? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            generateKeystoreAesKey()
        } else {
            generateAndWrapAesKey()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getKeystoreAesKey(): SecretKey? {
        return try {
            keyStore?.getKey(keyAlias, null) as? SecretKey
        } catch (e: Exception) {
            Logger.e(this, "Failed to retrieve Keystore AES key: ${e.message}")
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun generateKeystoreAesKey(): SecretKey? {
        return try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )

            keyGenerator.generateKey()
        } catch (e: Exception) {
            Logger.e(this, "Failed to generate Keystore AES key: ${e.message}")
            null
        }
    }

    private fun unwrapAesKey(): SecretKey? {
        return try {
            val wrappedKey = prefs.getString(PREF_WRAPPED_AES_KEY, "")

            if (wrappedKey.isEmpty()) {
                Logger.w(this, "No wrapped AES key found in preferences")
                return null
            }

            val rsaPrivateKey = getRsaPrivateKey() ?: run {
                Logger.w(this, "RSA private key unavailable for unwrapping AES key")
                return null
            }

            val cipher = Cipher.getInstance(RSA_CIPHER)
            cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey)
            val aesKeyBytes = cipher.doFinal(Base64.decode(wrappedKey, Base64.NO_WRAP))
            SecretKeySpec(aesKeyBytes, "AES")
        } catch (e: Exception) {
            Logger.w(this, "Failed to unwrap AES key: ${e.message}")
            null
        }
    }

    private fun generateAndWrapAesKey(): SecretKey? {
        return try {
            val rsaPublicKey = getOrCreateRsaKeyPair()?.public ?: run {
                Logger.e(this, "RSA public key unavailable, cannot wrap AES key")
                return null
            }

            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val aesKey = keyGenerator.generateKey()

            val cipher = Cipher.getInstance(RSA_CIPHER)
            cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey)
            val wrappedBytes = cipher.doFinal(aesKey.encoded)

            prefs.setString(PREF_WRAPPED_AES_KEY, Base64.encodeToString(wrappedBytes, Base64.NO_WRAP))
            aesKey
        } catch (e: Exception) {
            Logger.e(this, "Failed to generate and wrap AES key: ${e.message}")
            null
        }
    }

    private fun getRsaPrivateKey(): PrivateKey? {
        return try {
            keyStore?.getKey(rsaKeyAlias, null) as? PrivateKey
        } catch (e: Exception) {
            Logger.e(this, "Failed to retrieve RSA private key: ${e.message}")
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun getOrCreateRsaKeyPair(): KeyPair? {
        return try {
            keyStore?.let { ks ->
                if (ks.containsAlias(rsaKeyAlias)) {
                    val privateKey = ks.getKey(rsaKeyAlias, null) as? PrivateKey
                    val publicKey = ks.getCertificate(rsaKeyAlias)?.publicKey

                    if (privateKey != null && publicKey != null) {
                        return KeyPair(publicKey, privateKey)
                    }

                    Logger.w(this, "Existing RSA key pair is incomplete, regenerating")
                    ks.deleteEntry(rsaKeyAlias)
                    prefs.remove(PREF_WRAPPED_AES_KEY)
                }

                val start = Calendar.getInstance()
                val end = Calendar.getInstance().apply { add(Calendar.YEAR, 25) }

                val spec = android.security.KeyPairGeneratorSpec.Builder(context)
                    .setAlias(rsaKeyAlias)
                    .setSubject(X500Principal("CN=$rsaKeyAlias"))
                    .setSerialNumber(BigInteger.ONE)
                    .setStartDate(start.time)
                    .setEndDate(end.time)
                    .build()

                val keyPairGenerator = KeyPairGenerator.getInstance("RSA", ANDROID_KEYSTORE)
                keyPairGenerator.initialize(spec)
                keyPairGenerator.generateKeyPair()
            }
        } catch (e: Exception) {
            Logger.e(this, "Failed to create RSA key pair: ${e.message}")
            null
        }
    }

    private fun encryptWith(key: SecretKey, plaintext: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val ivEncoded = Base64.encodeToString(iv, Base64.NO_WRAP)
        val ctEncoded = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        return "$ivEncoded$SEPARATOR$ctEncoded"
    }

    private fun tryRegenerateAndEncrypt(plaintext: String): String? {
        deleteKey()

        return try {
            val key = createAesKey() ?: run {
                Logger.e(this, "Key regeneration failed: unable to create new AES key")
                return null
            }
            encryptWith(key, plaintext)
        } catch (e: Exception) {
            Logger.e(this, "Key regeneration and encryption failed: ${e.message}")
            null
        }
    }
}

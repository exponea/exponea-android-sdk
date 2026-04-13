package com.exponea.sdk.testutil

import android.security.keystore.KeyGenParameterSpec
import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.Provider
import java.security.SecureRandom
import java.security.cert.Certificate
import java.util.Collections
import java.util.Date
import java.util.Enumeration
import javax.crypto.KeyGeneratorSpi
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Shared in-memory storage backing the fake AndroidKeyStore provider.
 * Static so that [FakeKeyStoreSpi] and [FakeAesKeyGeneratorSpi] share state
 * the same way the real AndroidKeyStore does across KeyStore and KeyGenerator.
 */
internal object FakeKeyStoreState {
    val keys = mutableMapOf<String, Key>()

    fun reset() = keys.clear()
}

internal class FakeKeyStoreSpi : java.security.KeyStoreSpi() {
    override fun engineGetKey(alias: String, password: CharArray?): Key? =
        FakeKeyStoreState.keys[alias]

    override fun engineSetKeyEntry(
        alias: String,
        key: Key,
        password: CharArray?,
        chain: Array<out Certificate>?
    ) {
        FakeKeyStoreState.keys[alias] = key
    }

    override fun engineDeleteEntry(alias: String) {
        FakeKeyStoreState.keys.remove(alias)
    }

    override fun engineContainsAlias(alias: String): Boolean = alias in FakeKeyStoreState.keys
    override fun engineAliases(): Enumeration<String> =
        Collections.enumeration(FakeKeyStoreState.keys.keys)
    override fun engineSize(): Int = FakeKeyStoreState.keys.size
    override fun engineIsKeyEntry(alias: String): Boolean = alias in FakeKeyStoreState.keys
    override fun engineIsCertificateEntry(alias: String): Boolean = false
    override fun engineGetCertificate(alias: String): Certificate? = null
    override fun engineGetCertificateAlias(cert: Certificate): String? = null
    override fun engineGetCertificateChain(alias: String): Array<Certificate>? = null
    override fun engineGetCreationDate(alias: String): Date = Date()
    override fun engineLoad(stream: InputStream?, password: CharArray?) {}
    override fun engineStore(stream: OutputStream?, password: CharArray?) {}
    override fun engineSetCertificateEntry(alias: String, cert: Certificate) {}
    override fun engineSetKeyEntry(alias: String, key: ByteArray, chain: Array<out Certificate>?) {}
}

internal class FakeAesKeyGeneratorSpi : KeyGeneratorSpi() {
    private var keySize = 256
    private var alias: String? = null

    override fun engineInit(random: SecureRandom?) {}

    override fun engineInit(params: java.security.spec.AlgorithmParameterSpec?, random: SecureRandom?) {
        if (params is KeyGenParameterSpec) {
            alias = params.keystoreAlias
            keySize = if (params.keySize > 0) params.keySize else 256
        }
    }

    override fun engineInit(keysize: Int, random: SecureRandom?) {
        keySize = keysize
    }

    override fun engineGenerateKey(): SecretKey {
        val keyBytes = ByteArray(keySize / 8)
        SecureRandom().nextBytes(keyBytes)
        val key = SecretKeySpec(keyBytes, "AES")
        alias?.let { FakeKeyStoreState.keys[it] = key }
        return key
    }
}

internal class FakeAndroidKeyStoreProvider : Provider(NAME, 1.0, "Fake AndroidKeyStore for unit tests") {
    companion object {
        const val NAME = "AndroidKeyStore"
    }

    init {
        putService(object : Service(
            this, "KeyStore", "AndroidKeyStore",
            FakeKeyStoreSpi::class.java.name, null, null
        ) {
            override fun newInstance(constructorParameter: Any?): Any = FakeKeyStoreSpi()
        })
        putService(object : Service(
            this, "KeyGenerator", "AES",
            FakeAesKeyGeneratorSpi::class.java.name, null, null
        ) {
            override fun newInstance(constructorParameter: Any?): Any = FakeAesKeyGeneratorSpi()
        })
    }
}

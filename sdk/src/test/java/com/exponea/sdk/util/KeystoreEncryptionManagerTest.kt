package com.exponea.sdk.util

import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.testutil.FakeAndroidKeyStoreProvider
import com.exponea.sdk.testutil.FakeKeyStoreState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import java.security.Security
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
internal class KeystoreEncryptionManagerTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun registerFakeProvider() {
            Security.insertProviderAt(FakeAndroidKeyStoreProvider(), 1)
        }

        @JvmStatic
        @AfterClass
        fun unregisterFakeProvider() {
            Security.removeProvider(FakeAndroidKeyStoreProvider.NAME)
        }
    }

    private lateinit var prefs: ExponeaPreferences
    private lateinit var manager: KeystoreEncryptionManager

    @Before
    fun setUp() {
        FakeKeyStoreState.reset()
        mockkObject(Logger)
        every { Logger.w(any(), any()) } returns Unit
        every { Logger.e(any(), any()) } returns Unit
        prefs = mockk<ExponeaPreferences>(relaxed = true)
        every { prefs.getString(any(), any()) } returns ""
        manager = KeystoreEncryptionManager(
            context = RuntimeEnvironment.getApplication(),
            prefs = prefs,
            keyAlias = "test_key"
        )
    }

    @After
    fun tearDown() {
        manager.deleteKey()
        unmockkObject(Logger)
    }

    @Test
    fun `encrypt should return non-null ciphertext`() {
        val result = manager.encrypt("hello")
        assertThat(result, notNullValue())
    }

    @Test
    fun `encrypt result should contain iv and ciphertext separated by colon`() {
        val result = manager.encrypt("hello")!!
        val parts = result.split(":")
        assertThat(parts.size, equalTo(2))
        assertThat(parts[0].isEmpty(), equalTo(false))
        assertThat(parts[1].isEmpty(), equalTo(false))
    }

    @Test
    fun `decrypt should restore original plaintext`() {
        val plaintext = "secret-token-value"
        val encrypted = manager.encrypt(plaintext)!!
        val decrypted = manager.decrypt(encrypted)
        assertThat(decrypted, equalTo(plaintext))
    }

    @Test
    fun `encrypt-decrypt round trip should work with empty string`() {
        val encrypted = manager.encrypt("")!!
        assertThat(manager.decrypt(encrypted), equalTo(""))
    }

    @Test
    fun `encrypt-decrypt round trip should work with unicode`() {
        val plaintext = "tøkén with émojis 🔑"
        val encrypted = manager.encrypt(plaintext)!!
        assertThat(manager.decrypt(encrypted), equalTo(plaintext))
    }

    @Test
    fun `encrypt-decrypt round trip should work with long string`() {
        val plaintext = "a".repeat(10_000)
        val encrypted = manager.encrypt(plaintext)!!
        assertThat(manager.decrypt(encrypted), equalTo(plaintext))
    }

    @Test
    fun `decrypt should return null for malformed input without colon`() {
        assertThat(manager.decrypt("no-colon-here"), nullValue())
    }

    @Test
    fun `decrypt should return null for empty string`() {
        assertThat(manager.decrypt(""), nullValue())
    }

    @Test
    fun `decrypt should return null for input with too many colons`() {
        assertThat(manager.decrypt("a:b:c"), nullValue())
    }

    @Test
    fun `decrypt should return null for corrupted ciphertext`() {
        val encrypted = manager.encrypt("hello")!!
        val parts = encrypted.split(":")
        val corrupted = parts[0] + ":" + "AAAA"
        assertThat(manager.decrypt(corrupted), nullValue())
    }

    @Test
    fun `decrypt should return null for corrupted IV`() {
        val encrypted = manager.encrypt("hello")!!
        val parts = encrypted.split(":")
        val corrupted = "AAAA:" + parts[1]
        assertThat(manager.decrypt(corrupted), nullValue())
    }

    @Test
    fun `decrypt should log warning for malformed input`() {
        manager.decrypt("no-colon")
        verify {
            Logger.w(any(), match { it.contains("malformed input") })
        }
    }

    @Test
    fun `decrypt should return null after deleteKey`() {
        val encrypted = manager.encrypt("hello")!!
        manager.deleteKey()
        assertThat(manager.decrypt(encrypted), nullValue())
    }

    @Test
    fun `deleteKey should remove wrapped AES key from preferences`() {
        manager.deleteKey()
        verify { prefs.remove("exponea_wrapped_aes_key") }
    }

    @Test
    fun `encrypt should still work after deleteKey`() {
        manager.encrypt("before")
        manager.deleteKey()
        val encrypted = manager.encrypt("after")
        assertThat(encrypted, notNullValue())
        assertThat(manager.decrypt(encrypted!!), equalTo("after"))
    }

    @Test
    fun `encrypt should succeed even after key is deleted between calls`() {
        val first = manager.encrypt("first")
        assertThat(first, notNullValue())
        manager.deleteKey()
        val second = manager.encrypt("second")
        assertThat(second, notNullValue())
        assertThat(manager.decrypt(second!!), equalTo("second"))
    }

    @Test
    fun `old ciphertext should not decrypt after key regeneration`() {
        val encrypted = manager.encrypt("old-data")!!
        manager.deleteKey()
        manager.encrypt("triggers-new-key")
        assertThat(manager.decrypt(encrypted), nullValue())
    }
}

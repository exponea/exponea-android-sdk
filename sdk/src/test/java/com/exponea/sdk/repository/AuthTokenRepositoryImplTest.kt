package com.exponea.sdk.repository

import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.KeystoreEncryptionManager
import com.exponea.sdk.util.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class AuthTokenRepositoryImplTest {

    private lateinit var prefs: ExponeaPreferences
    private lateinit var encryption: KeystoreEncryptionManager
    private lateinit var repository: AuthTokenRepository

    @Before
    fun setUp() {
        mockkObject(Logger)
        every { Logger.w(any(), any()) } returns Unit
        every { Logger.e(any(), any()) } returns Unit
        prefs = mockk<ExponeaPreferences>(relaxed = true)
        encryption = mockk<KeystoreEncryptionManager>(relaxed = true)
        every { prefs.getString(any(), any()) } returns ""
        every { prefs.getBoolean(any(), any()) } returns false
        every { encryption.encrypt(any()) } returns null
        repository = AuthTokenRepositoryImpl(prefs = prefs, encryption = encryption)
    }

    @After
    fun tearDown() {
        unmockkObject(Logger)
    }

    @Test
    fun `getToken should return null when no token has been set`() {
        assertThat(repository.getToken(), nullValue())
    }

    @Test
    fun `getToken should return stored token after setToken`() {
        repository.setToken("test-token")

        assertThat(repository.getToken(), equalTo("test-token"))
    }

    @Test
    fun `setToken should overwrite previously stored token`() {
        repository.setToken("first-token")
        repository.setToken("second-token")

        assertThat(repository.getToken(), equalTo("second-token"))
    }

    @Test
    fun `clear should remove stored token`() {
        repository.setToken("test-token")
        repository.clear()
        assertThat(repository.getToken(), nullValue())
    }

    @Test
    fun `setToken should persist encrypted token when Keystore is available`() {
        val token = "my-jwt"
        val encryptedText = "ciphertext"

        every { encryption.encrypt(token) } returns encryptedText

        repository.setToken(token)

        verify { prefs.setString("exponea_auth_token", encryptedText) }
        verify { prefs.setBoolean("exponea_auth_token_encrypted", true) }
    }

    @Test
    fun `setToken should persist plaintext when Keystore is unavailable`() {
        val token = "my-jwt"

        every { encryption.encrypt(token) } returns null

        repository.setToken(token)

        verify { prefs.setString("exponea_auth_token", token) }
        verify { prefs.setBoolean("exponea_auth_token_encrypted", false) }

        val messageSlot = slot<String>()

        verify { Logger.w(any(), capture(messageSlot)) }
        assertThat(
            messageSlot.captured,
            equalTo("Keystore encryption unavailable, storing token without encryption")
        )
    }

    @Test
    fun `getToken should load and decrypt persisted encrypted token on first access`() {
        val encryptedText = "ciphertext"

        every { prefs.getString("exponea_auth_token", "") } returns encryptedText
        every { prefs.getBoolean("exponea_auth_token_encrypted", false) } returns true
        every { encryption.decrypt(encryptedText) } returns "restored-jwt"

        val repo = AuthTokenRepositoryImpl(prefs = prefs, encryption = encryption)

        assertThat(repo.getToken(), equalTo("restored-jwt"))
    }

    @Test
    fun `getToken should load plaintext persisted token on first access`() {
        val plainText = "plain-jwt"

        every { prefs.getString("exponea_auth_token", "") } returns plainText
        every { prefs.getBoolean("exponea_auth_token_encrypted", false) } returns false

        val repo = AuthTokenRepositoryImpl(prefs = prefs, encryption = encryption)

        assertThat(repo.getToken(), equalTo(plainText))
    }

    @Test
    fun `getToken should clear data when decryption fails on first access`() {
        val corruptedData = "corrupted-data"
        every { prefs.getString("exponea_auth_token", "") } returns corruptedData
        every { prefs.getBoolean("exponea_auth_token_encrypted", false) } returns true
        every { encryption.decrypt(corruptedData) } returns null

        val repo = AuthTokenRepositoryImpl(prefs = prefs, encryption = encryption)
        assertThat(repo.getToken(), nullValue())

        val messageSlot = slot<String>()

        verify { Logger.w(any(), capture(messageSlot)) }
        assertThat(
            messageSlot.captured,
            equalTo("Failed to decrypt persisted token, clearing stored data")
        )
        verify { encryption.deleteKey() }
        verify { prefs.remove("exponea_auth_token") }
        verify { prefs.remove("exponea_auth_token_encrypted") }
    }

    @Test
    fun `clear should remove persisted data`() {
        repository.setToken("test-token")
        repository.clear()

        verify { prefs.remove("exponea_auth_token") }
        verify { prefs.remove("exponea_auth_token_encrypted") }
    }
}

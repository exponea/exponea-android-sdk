package com.exponea.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.exceptions.InvalidConfigurationException
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.testutil.ExponeaSDKTest
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConfigurationTest : ExponeaSDKTest() {

    private fun setupExponea(authorization: String) {
        val configuration = ExponeaConfiguration(projectToken = "projectToken", authorization = authorization)
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(ApplicationProvider.getApplicationContext(), configuration)
    }

    @Rule @JvmField
    val expectedException = ExpectedException.none()

    @Test
    fun `should initialize with correct authorization`() {
        setupExponea("Token asdf")
        assertEquals(Exponea.isInitialized, true)
    }

    @Test
    fun `should throw error when initializing sdk with basic authorization`() {
        expectedException.expect(InvalidConfigurationException::class.java)
        expectedException.expectMessage("Basic authentication is not supported by mobile SDK for security reasons.")
        setupExponea("Basic asdf")
        assertEquals(Exponea.isInitialized, true)
    }

    @Test
    fun `should throw error when initializing sdk with unknown authorization`() {
        expectedException.expect(InvalidConfigurationException::class.java)
        expectedException.expectMessage("Use 'Token <access token>' as authorization for SDK.")
        setupExponea("asdf")
        assertEquals(Exponea.isInitialized, true)
    }

    @Test
    fun `should initialize SDK from file`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.initFromFile(context)
        assertEquals(Exponea.isInitialized, true)
    }
}

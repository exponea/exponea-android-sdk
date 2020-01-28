package com.exponea.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.testutil.ExponeaSDKTest
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConfigurationTest : ExponeaSDKTest() {

    private fun setupConfigurationWithStruct() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration()
        configuration.projectToken = "projectToken"
        configuration.authorization = "projectAuthorization"
        skipInstallEvent()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)
    }

    private fun setupConfigurationWithFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.initFromFile(context)
    }

    @Test
    fun InstantiateSDKWithConfig_ShouldPass() {
        setupConfigurationWithStruct()
        assertEquals(Exponea.isInitialized, true)
    }

    @Test
    fun InstantiateSDKWithFile_ShouldPass() {
        setupConfigurationWithFile()
        assertEquals(Exponea.isInitialized, true)
    }
}

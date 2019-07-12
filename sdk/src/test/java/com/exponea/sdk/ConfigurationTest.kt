package com.exponea.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.ExponeaConfiguration
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ConfigurationTest {

    private fun setupConfigurationWithStruct() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration()
        configuration.projectToken = "projectToken"
        configuration.authorization = "projectAuthorization"
        Exponea.init(context, configuration)
    }

    private fun setupConfigurationWithFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
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
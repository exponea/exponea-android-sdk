package com.exponea.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.util.currentTimeSeconds
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*
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
    fun TestData() {
        val timestamp = currentTimeSeconds()
        println("Timestamp: $timestamp")
        println("Timestamp: ${Calendar.getInstance().timeInMillis / 1000}")
    }

    @Test
    fun InstantiateSDKWithFile_ShouldPass() {
        setupConfigurationWithFile()
        assertEquals(Exponea.isInitialized, true)
    }
}
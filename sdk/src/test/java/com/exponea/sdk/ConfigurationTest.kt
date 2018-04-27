package com.exponea.sdk

import com.exponea.sdk.models.ExponeaConfiguration
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ConfigurationTest {

    private fun initializeExponeaWithoutConfig() {
        val context = RuntimeEnvironment.application
        Exponea.init(context, null, null)
    }

    private fun setupConfigurationWithStruct() {
        val context = RuntimeEnvironment.application
        val configuration = ExponeaConfiguration()
        configuration.projectToken = "projectToken"
        configuration.authorization = "projectAuthorization"
        Exponea.init(context, configuration, null)
    }

    private fun setupConfigurationWithFile() {
        val context = RuntimeEnvironment.application
        Exponea.init(context, null, "config_valid.xml")
    }

    @Test
    fun InstantiateSDKWithoutConfig_ShouldFail() {
        initializeExponeaWithoutConfig()
        assertEquals(Exponea.isInitialized, false)
    }

    @Test
    fun InstantiateSDKWithConfig_ShouldPass() {
        setupConfigurationWithStruct()
        assertEquals(Exponea.isInitialized, true)
    }

//    @Test
//    TODO: Finish test loading from file.
//    fun InstantiateSDKWithFile_ShouldPass() {
//        setupConfigurationWithFile()
//        assertEquals(Exponea.isInitialized, true)
//    }
}
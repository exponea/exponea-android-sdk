package com.exponea.sdk

import com.exponea.sdk.models.ExponeaConfiguration
import org.bouncycastle.util.Times
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.sql.Timestamp
import java.text.DateFormat
import java.util.*
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ConfigurationTest {

    private fun initializeExponeaWithoutConfig() {
        val context = RuntimeEnvironment.application
        try {
            Exponea.init(context, "")
        } catch (e: Exception) {

        }
    }

    private fun setupConfigurationWithStruct() {
        val context = RuntimeEnvironment.application
        val configuration = ExponeaConfiguration()
        configuration.projectToken = "projectToken"
        configuration.authorization = "projectAuthorization"
        Exponea.init(context, configuration)
    }

    private fun setupConfigurationWithFile() {
        val context = RuntimeEnvironment.application
        Exponea.init(context, "config_valid.xml")
    }

    @Test
    fun InstantiateSDKWithoutConfig_ShouldFail() {
        initializeExponeaWithoutConfig()
        assertEquals(Exponea.isInitialized, true)
    }

    @Test
    fun InstantiateSDKWithConfig_ShouldPass() {
        setupConfigurationWithStruct()
        assertEquals(Exponea.isInitialized, true)
    }

    @Test
    fun TestData() {
        val timestamp = Date().time
        println("Timestamp: ${timestamp}")
        println("Timestamp: ${Calendar.getInstance().timeInMillis}")

        //val cal =
        println("Timestamp to Date: ${Date(timestamp.toLong() * 1000)}")

        println("Timestamp to Date 2: ${ Date(timestamp) }")

        val newStamp = 424515600000
        println("New timestamp to Date 2: ${ Date(newStamp.toLong()) }")

    }

//    @Test
//    TODO: Finish test loading from file.
//    fun InstantiateSDKWithFile_ShouldPass() {
//        setupConfigurationWithFile()
//        assertEquals(Exponea.isInitialized, true)
//    }
}
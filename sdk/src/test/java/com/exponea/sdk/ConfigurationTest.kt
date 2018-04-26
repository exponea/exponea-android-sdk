package com.exponea.sdk

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals

class ConfigurationTest {

    @Before
    public fun setup() {
        val context = RuntimeEnvironment.application
        Exponea.init(context, null, null )
    }

    @After
    public fun tearDown() {

    }

    @Test
    fun InstantiateSDKWithoutConfig_shouldFail() {
        //assertEquals(Exponea.isInitialized, false)
    }
}
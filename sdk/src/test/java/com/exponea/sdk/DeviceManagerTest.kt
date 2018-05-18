package com.exponea.sdk

import com.exponea.sdk.manager.DeviceManager
import com.exponea.sdk.manager.DeviceManagerImpl
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

@RunWith(RobolectricTestRunner::class)
class DeviceManagerTest {

    lateinit var deviceManager: DeviceManager

    @Test
    @Config(qualifiers = "large")
    fun testGetDeviceTypeTablet_ShouldPass(){
        deviceManager = DeviceManagerImpl(RuntimeEnvironment.application.applicationContext)
        val tabletType = deviceManager.getDeviceType()
        assertEquals("tablet", tabletType)
    }

    @Test
    @Config(qualifiers = "normal")
    fun testGetDeviceTypeMobile_ShouldPass() {
        deviceManager = DeviceManagerImpl(RuntimeEnvironment.application.applicationContext)
        val type = deviceManager.getDeviceType()
        assertEquals("mobile", type)
    }

    @Test
    @Config(qualifiers = "normal")
    fun testIsTablet_AssertFalse() {
        deviceManager = DeviceManagerImpl(RuntimeEnvironment.application.applicationContext)
        assertEquals(false, deviceManager.isTablet())
    }

    @Test
    @Config(qualifiers = "large")
    fun testIsTablet_AssertTrue() {
        deviceManager = DeviceManagerImpl(RuntimeEnvironment.application.applicationContext)
        assertEquals(true, deviceManager.isTablet())
    }
}
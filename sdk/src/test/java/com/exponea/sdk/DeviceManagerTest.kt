package com.exponea.sdk

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.manager.DeviceManager
import com.exponea.sdk.manager.DeviceManagerImpl
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class DeviceManagerTest {

    lateinit var deviceManager: DeviceManager

    @Test
    @Config(qualifiers = "large")
    fun testGetDeviceTypeTablet_ShouldPass(){
        deviceManager = DeviceManagerImpl(ApplicationProvider.getApplicationContext())
        val tabletType = deviceManager.getDeviceType()
        assertEquals("tablet", tabletType)
    }

    @Test
    @Config(qualifiers = "normal")
    fun testGetDeviceTypeMobile_ShouldPass() {
        deviceManager = DeviceManagerImpl(ApplicationProvider.getApplicationContext())
        val type = deviceManager.getDeviceType()
        assertEquals("mobile", type)
    }

    @Test
    @Config(qualifiers = "normal")
    fun testIsTablet_AssertFalse() {
        deviceManager = DeviceManagerImpl(ApplicationProvider.getApplicationContext())
        assertEquals(false, deviceManager.isTablet())
    }

    @Test
    @Config(qualifiers = "large")
    fun testIsTablet_AssertTrue() {
        deviceManager = DeviceManagerImpl(ApplicationProvider.getApplicationContext())
        assertEquals(true, deviceManager.isTablet())
    }
}
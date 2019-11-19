package com.exponea.sdk.models

import android.app.Application
import android.content.pm.PackageInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.BuildConfig
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class DevicePropertiesTest {

    @Test
    fun `should create device properties from context`() {
        assertEquals(
            DeviceProperties(
                osName = "Android",
                osVersion = Build.VERSION.RELEASE,
                sdk = "AndroidSDK",
                sdkVersion = BuildConfig.VERSION_NAME,
                deviceModel = Build.MODEL,
                deviceType = "mobile",
                appVersion = ""
            ),
            DeviceProperties(ApplicationProvider.getApplicationContext())
        )
    }

    @Test
    fun `should get app version from package manager`() {
        val packageManager = shadowOf(ApplicationProvider.getApplicationContext<Application>().packageManager)
        val packageInfo = PackageInfo()
        packageInfo.packageName = "com.exponea.sdk.test"
        packageInfo.versionName = "mock version name"
        packageManager.installPackage(packageInfo)
        assertEquals("mock version name", DeviceProperties(ApplicationProvider.getApplicationContext()).appVersion)
    }

    @Test
    @Config(qualifiers = "large")
    fun `device type should be tablet on large device`() {
        assertEquals("tablet", DeviceProperties(ApplicationProvider.getApplicationContext()).deviceType)
    }

    @Test
    @Config(qualifiers = "normal")
    fun `device type should be mobile on normal device`() {
        assertEquals("mobile", DeviceProperties(ApplicationProvider.getApplicationContext()).deviceType)
    }
}

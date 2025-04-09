package com.exponea.sdk

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.DeviceInitiatedRepository
import com.exponea.sdk.repository.DeviceInitiatedRepositoryImpl
import com.exponea.sdk.testutil.ExponeaSDKTest
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DeviceInitiatedRepositoryTest : ExponeaSDKTest() {

    private lateinit var repo: DeviceInitiatedRepository

    @Before
    fun init() {
        val prefs = ExponeaPreferencesImpl(ApplicationProvider.getApplicationContext())
        repo = DeviceInitiatedRepositoryImpl(prefs)
    }

    @Test
    fun testGet_ShouldPassed() {
        val value = true
        repo.set(value)
        assertEquals(true, repo.get())
    }

    @Test
    fun `should not set flag if SDK is stopped`() {
        val value = true
        Exponea.isStopped = true
        repo.set(value)
        assertEquals(false, repo.get())
    }
}

package com.exponea.sdk

import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.DeviceInitiatedRepository
import com.exponea.sdk.repository.DeviceInitiatedRepositoryImpl
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class DeviceInitiatedRepositoryTest {

    lateinit var repo : DeviceInitiatedRepository

    @Before
    fun init() {
        val prefs = ExponeaPreferencesImpl(RuntimeEnvironment.application.applicationContext)
        repo = DeviceInitiatedRepositoryImpl(prefs)
    }

    @Test
    fun testGet_ShouldPassed() {
        val value = true
        repo.set(value)
        assertEquals(true, repo.get())
    }

}
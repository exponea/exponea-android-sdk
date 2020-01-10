package com.exponea.sdk.repository

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.InAppMessageDisplayState
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.google.gson.Gson
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppMessageDisplayStateRepositoryImplTest {
    private lateinit var repo: InAppMessageDisplayStateRepository
    private val message = InAppMessageTest.getInAppMessage()

    @Before
    fun before() {
        repo = InAppMessageDisplayStateRepositoryImpl(
            ExponeaPreferencesImpl(ApplicationProvider.getApplicationContext()),
            Gson()
        )
    }

    @Test
    fun `should return empty data`() {
        assertEquals(InAppMessageDisplayState(null, null), repo.get(InAppMessageTest.getInAppMessage()))
    }

    @Test
    fun `should save data`() {
        repo.setDisplayed(message, Date(1000))
        assertEquals(Date(1000), repo.get(message).displayed)
        repo.setInteracted(message, Date(2000))
        assertEquals(InAppMessageDisplayState(Date(1000), Date(2000)), repo.get(message))
        repo.setDisplayed(message, Date(3000))
        assertEquals(InAppMessageDisplayState(Date(3000), Date(2000)), repo.get(message))
    }

    @Test
    fun `should keep data between instances`() {
        // need to round of milliseconds that would get lost in json serialization
        val now = Date.from(Date().toInstant().truncatedTo(ChronoUnit.SECONDS))
        repo.setDisplayed(message, now)
        repo = InAppMessageDisplayStateRepositoryImpl(
            ExponeaPreferencesImpl(ApplicationProvider.getApplicationContext()),
            Gson()
        )
        assertEquals(InAppMessageDisplayState(now, null), repo.get(message))
    }

    @Test
    fun `should delete old data`() {
        repo.setDisplayed(message, Date(1000))
        repo = InAppMessageDisplayStateRepositoryImpl(
            ExponeaPreferencesImpl(ApplicationProvider.getApplicationContext()),
            Gson()
        )
        assertEquals(InAppMessageDisplayState(null, null), repo.get(message))
    }
}

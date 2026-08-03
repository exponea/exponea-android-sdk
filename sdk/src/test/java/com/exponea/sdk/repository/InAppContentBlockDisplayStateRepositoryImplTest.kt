package com.exponea.sdk.repository

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.manager.InAppContentBlockManagerImplTest.Companion.buildMessage
import com.exponea.sdk.models.InAppContentBlockDisplayState
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.util.ExponeaGson
import java.util.Date
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppContentBlockDisplayStateRepositoryImplTest {
    private lateinit var prefs: ExponeaPreferences
    private lateinit var repo: InAppContentBlockDisplayStateRepository
    private val message = buildMessage(id = "content-block-id")

    @Before
    fun before() {
        prefs = ExponeaPreferencesImpl(ApplicationProvider.getApplicationContext())
        prefs.remove(InAppContentBlockDisplayStateRepositoryImpl.KEY)
        repo = InAppContentBlockDisplayStateRepositoryImpl(prefs, ExponeaGson.instance)
    }

    @Test
    fun `should update cache and snapshot on writes`() {
        repo.setDisplayed(message, Date(1000))
        assertEquals(
            InAppContentBlockDisplayState(Date(1000), 1, null, 0),
            repo.get(message)
        )

        repo.setInteracted(message, Date(2000))
        val expected = InAppContentBlockDisplayState(Date(1000), 1, Date(2000), 1)
        assertEquals(expected, repo.get(message))
        assertEquals(expected, repo.getAll()[message.id])
    }

    @Test
    fun `should persist cached state for next repository instance`() {
        val now = Date()
        repo.setDisplayed(message, now)

        val restoredRepo = InAppContentBlockDisplayStateRepositoryImpl(
            ExponeaPreferencesImpl(ApplicationProvider.getApplicationContext()),
            ExponeaGson.instance
        )

        assertEquals(InAppContentBlockDisplayState(now, 1, null, 0), restoredRepo.get(message))
    }

    @Test
    fun `should not observe external preference mutation after initialization`() {
        repo.setDisplayed(message, Date(1000))
        prefs.setString(InAppContentBlockDisplayStateRepositoryImpl.KEY, "{}")

        assertEquals(
            InAppContentBlockDisplayState(Date(1000), 1, null, 0),
            repo.get(message)
        )
    }

    @Test
    fun `should clear cache and preferences`() {
        repo.setDisplayed(message, Date(1000))
        repo.clear()

        assertEquals(InAppContentBlockDisplayState(null, 0, null, 0), repo.get(message))

        val restoredRepo = InAppContentBlockDisplayStateRepositoryImpl(
            ExponeaPreferencesImpl(ApplicationProvider.getApplicationContext()),
            ExponeaGson.instance
        )
        assertEquals(InAppContentBlockDisplayState(null, 0, null, 0), restoredRepo.get(message))
    }

    @Test
    fun `should delete old states from cache and preferences`() {
        val oldMessage = buildMessage(id = "old-content-block-id")
        val recentMessage = buildMessage(id = "recent-content-block-id")
        val recentDate = Date()
        repo.setDisplayed(oldMessage, Date(1000))
        repo.setDisplayed(recentMessage, recentDate)

        val restoredRepo = InAppContentBlockDisplayStateRepositoryImpl(
            ExponeaPreferencesImpl(ApplicationProvider.getApplicationContext()),
            ExponeaGson.instance
        )

        assertEquals(InAppContentBlockDisplayState(null, 0, null, 0), restoredRepo.get(oldMessage))
        assertEquals(InAppContentBlockDisplayState(recentDate, 1, null, 0), restoredRepo.get(recentMessage))
        assertEquals(setOf(recentMessage.id), restoredRepo.getAll().keys)
    }
}

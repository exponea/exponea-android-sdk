package com.exponea.sdk.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.Route
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.repository.EventRepositoryImpl
import com.exponea.sdk.testutil.ExponeaSDKTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class TemporaryEventRepositoryImplTest : ExponeaSDKTest() {
    private lateinit var storedRepo: EventRepository
    private lateinit var temporaryRepo: TemporaryEventRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        storedRepo = EventRepositoryImpl(context)
        storedRepo.clear()
        temporaryRepo = TemporaryEventRepositoryImpl(context)
    }

    private fun event(id: String, timestamp: Double? = 100.0, shouldBeSkipped: Boolean = false) = ExportedEvent(
        id = id,
        route = Route.TRACK_EVENTS,
        timestamp = timestamp,
        shouldBeSkipped = shouldBeSkipped
    )

    @Test
    fun `should count and list only events added through it`() {
        storedRepo.add(event("stored"))
        temporaryRepo.add(event("a"))
        temporaryRepo.add(event("b"))

        assertThat(temporaryRepo.count(), equalTo(2))
        assertThat(temporaryRepo.all().map { it.id }, containsInAnyOrder("a", "b"))
        assertThat(storedRepo.count(), equalTo(3))
    }

    @Test
    fun `should return oldest not skipped event of its own`() {
        storedRepo.add(event("stored", timestamp = 1.0))
        temporaryRepo.add(event("c", timestamp = 300.0))
        temporaryRepo.add(event("a", timestamp = 100.0))
        temporaryRepo.add(event("b", timestamp = 200.0))

        assertThat(temporaryRepo.nextToFlush()?.id, equalTo("a"))

        temporaryRepo.markSkipped("a")
        assertThat(temporaryRepo.nextToFlush()?.id, equalTo("b"))
    }

    @Test
    fun `nextToFlush should keep insertion order for equal timestamps`() {
        // Ids are inserted against their hash order, so a map iterating in hash order would return "a" first
        val ids = listOf("c", "b", "a")
        ids.forEach { temporaryRepo.add(event(it, timestamp = 100.0)) }

        val flushed = ids.map {
            val next = temporaryRepo.nextToFlush()
            temporaryRepo.remove(next!!.id)
            next.id
        }

        assertThat(flushed, equalTo(ids))
    }

    @Test
    fun `should return event without timestamp first`() {
        temporaryRepo.add(event("a", timestamp = 100.0))
        temporaryRepo.add(event("noTimestamp", timestamp = null))

        assertThat(temporaryRepo.nextToFlush()?.id, equalTo("noTimestamp"))
    }

    @Test
    fun `should return nothing when all own events are skipped`() {
        storedRepo.add(event("stored"))
        temporaryRepo.add(event("a"))
        temporaryRepo.markSkipped("a")

        assertThat(temporaryRepo.nextToFlush(), nullValue())
    }

    @Test
    fun `should skip event without counting a try on markSkipped`() {
        temporaryRepo.add(event("a"))

        temporaryRepo.markSkipped("a")

        assertThat(temporaryRepo.all().single().tries, equalTo(0))
        assertThat(temporaryRepo.all().single().shouldBeSkipped, equalTo(true))
        assertThat(storedRepo.get("a")?.tries, equalTo(0))
        assertThat(storedRepo.get("a")?.shouldBeSkipped, equalTo(true))
    }

    @Test
    fun `should count a try and skip event on markFailed in view and database`() {
        temporaryRepo.add(event("a"))

        temporaryRepo.markFailed("a")

        assertThat(temporaryRepo.all().single().tries, equalTo(1))
        assertThat(temporaryRepo.all().single().shouldBeSkipped, equalTo(true))
        assertThat(storedRepo.get("a")?.tries, equalTo(1))
        assertThat(storedRepo.get("a")?.shouldBeSkipped, equalTo(true))
    }

    @Test
    fun `should reset skipped flag only for its own events`() {
        storedRepo.add(event("stored", shouldBeSkipped = true))
        temporaryRepo.add(event("a"))
        temporaryRepo.markSkipped("a")

        temporaryRepo.resetSkipped()

        assertThat(temporaryRepo.all().single().shouldBeSkipped, equalTo(false))
        assertThat(storedRepo.get("a")?.shouldBeSkipped, equalTo(false))
        // events stored by someone else keep their state
        assertThat(storedRepo.get("stored")?.shouldBeSkipped, equalTo(true))
    }

    @Test
    fun `should remove event from its view and database`() {
        temporaryRepo.add(event("a"))
        temporaryRepo.add(event("b"))

        temporaryRepo.remove("a")

        assertThat(temporaryRepo.all().map { it.id }, contains("b"))
        assertThat(temporaryRepo.count(), equalTo(1))
        assertThat(storedRepo.get("a"), nullValue())
    }

    @Test
    fun `should ignore marking of unknown event`() {
        temporaryRepo.add(event("a"))

        temporaryRepo.markSkipped("unknown")
        temporaryRepo.markFailed("unknown")

        assertThat(temporaryRepo.count(), equalTo(1))
        assertThat(temporaryRepo.all().single().shouldBeSkipped, equalTo(false))
    }
}

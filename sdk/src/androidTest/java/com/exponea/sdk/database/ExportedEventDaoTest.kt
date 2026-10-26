package com.exponea.sdk.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.IntegrationConfigType
import com.exponea.sdk.models.IntegrationConfiguration
import com.exponea.sdk.models.Route
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportedEventDaoTest {
    private lateinit var database: ExponeaDatabase
    private lateinit var dao: ExportedEventDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ExponeaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.exportedEventDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun event(
        id: String,
        timestamp: Double? = 100.0,
        tries: Int = 0,
        shouldBeSkipped: Boolean = false
    ) = ExportedEvent(
        id = id,
        tries = tries,
        route = Route.TRACK_EVENTS,
        shouldBeSkipped = shouldBeSkipped,
        integrationConfiguration = IntegrationConfiguration(
            integrationId = "stream-id",
            baseUrl = "https://base.url",
            authorization = null,
            type = IntegrationConfigType.STREAM
        ),
        type = "type-$id",
        timestamp = timestamp,
        customerIds = hashMapOf("registered" to "user", "cookie" to "abc"),
        properties = hashMapOf("string" to "value", "number" to 1.5),
        sdkEventType = "TRACK_EVENT"
    )

    private fun ids(events: List<ExportedEvent>) = events.map { it.id }

    @Test
    fun addedEventShouldBeReadBackWithAllColumns() {
        val added = event("a")

        dao.add(added)

        assertThat(dao.get("a"), equalTo(added))
    }

    @Test
    fun getShouldReturnNullForUnknownId() {
        dao.add(event("a"))

        assertThat(dao.get("unknown"), nullValue())
    }

    @Test
    fun allAndCountShouldReflectStoredEvents() {
        assertThat(dao.count(), equalTo(0))
        assertThat(dao.all().size, equalTo(0))

        dao.add(event("a"))
        dao.add(event("b"))

        assertThat(dao.count(), equalTo(2))
        assertThat(ids(dao.all()).toSet(), equalTo(setOf("a", "b")))
    }

    @Test
    fun updateShouldReplaceStoredRow() {
        dao.add(event("a"))

        dao.update(event("a", tries = 3, shouldBeSkipped = true))

        val stored = dao.get("a")
        assertThat(stored?.tries, equalTo(3))
        assertThat(stored?.shouldBeSkipped, equalTo(true))
    }

    @Test
    fun deleteShouldRemoveOnlyGivenEvent() {
        dao.add(event("a"))
        dao.add(event("b"))

        dao.delete("a")

        assertThat(ids(dao.all()), equalTo(listOf("b")))
    }

    @Test
    fun deleteShouldIgnoreUnknownId() {
        dao.add(event("a"))

        dao.delete("unknown")

        assertThat(dao.count(), equalTo(1))
    }

    @Test
    fun clearShouldRemoveAllEvents() {
        dao.add(event("a"))
        dao.add(event("b"))

        dao.clear()

        assertThat(dao.count(), equalTo(0))
    }

    @Test
    fun nextToFlushShouldReturnNullForEmptyTable() {
        assertThat(dao.nextToFlush(), nullValue())
    }

    @Test
    fun nextToFlushShouldReturnOldestEvent() {
        dao.add(event("c", timestamp = 300.0))
        dao.add(event("a", timestamp = 100.0))
        dao.add(event("b", timestamp = 200.0))

        assertThat(dao.nextToFlush()?.id, equalTo("a"))
    }

    @Test
    fun nextToFlushShouldPreferEventWithoutTimestamp() {
        dao.add(event("a", timestamp = 100.0))
        dao.add(event("noTimestamp", timestamp = null))

        assertThat(dao.nextToFlush()?.id, equalTo("noTimestamp"))
    }

    @Test
    fun nextToFlushShouldKeepInsertionOrderForEqualTimestamps() {
        dao.add(event("first", timestamp = 100.0))
        dao.add(event("second", timestamp = 100.0))
        dao.add(event("third", timestamp = 100.0))

        assertThat(dao.nextToFlush()?.id, equalTo("first"))
        dao.delete("first")
        assertThat(dao.nextToFlush()?.id, equalTo("second"))
    }

    @Test
    fun nextToFlushShouldIgnoreSkippedEvents() {
        dao.add(event("a", timestamp = 100.0, shouldBeSkipped = true))
        dao.add(event("b", timestamp = 200.0))

        assertThat(dao.nextToFlush()?.id, equalTo("b"))
    }

    @Test
    fun nextToFlushShouldReturnNullWhenAllEventsAreSkipped() {
        dao.add(event("a", shouldBeSkipped = true))
        dao.add(event("b", shouldBeSkipped = true))

        assertThat(dao.nextToFlush(), nullValue())
    }

    @Test
    fun nextToFlushShouldDecodeAllColumns() {
        val added = event("a")
        dao.add(added)

        assertThat(dao.nextToFlush(), equalTo(added))
    }

    @Test
    fun markSkippedShouldSkipEventWithoutCountingTry() {
        dao.add(event("a", timestamp = 100.0, tries = 2))
        dao.add(event("b", timestamp = 200.0))

        dao.markSkipped("a")

        val stored = dao.get("a")
        assertThat(stored?.shouldBeSkipped, equalTo(true))
        assertThat(stored?.tries, equalTo(2))
        assertThat(dao.nextToFlush()?.id, equalTo("b"))
    }

    @Test
    fun markFailedShouldCountTryAndSkipEvent() {
        dao.add(event("a", timestamp = 100.0, tries = 2))
        dao.add(event("b", timestamp = 200.0))

        dao.markFailed("a")

        val stored = dao.get("a")
        assertThat(stored?.shouldBeSkipped, equalTo(true))
        assertThat(stored?.tries, equalTo(3))
        assertThat(dao.nextToFlush()?.id, equalTo("b"))
    }

    @Test
    fun markFailedShouldKeepOtherColumnsUntouched() {
        val added = event("a", tries = 1)
        dao.add(added)

        dao.markFailed("a")

        assertThat(dao.get("a"), equalTo(added.copy(tries = 2, shouldBeSkipped = true)))
    }

    @Test
    fun markingUnknownIdShouldChangeNothing() {
        dao.add(event("a"))

        dao.markSkipped("unknown")
        dao.markFailed("unknown")

        assertThat(dao.get("a"), equalTo(event("a")))
    }

    @Test
    fun resetSkippedShouldMakeAllEventsEligibleAgainWithoutChangingTries() {
        dao.add(event("a", timestamp = 100.0, tries = 1, shouldBeSkipped = true))
        dao.add(event("b", timestamp = 200.0, tries = 4, shouldBeSkipped = true))
        dao.add(event("c", timestamp = 300.0))

        dao.resetSkipped()

        assertThat(dao.all().none { it.shouldBeSkipped }, equalTo(true))
        assertThat(dao.get("a")?.tries, equalTo(1))
        assertThat(dao.get("b")?.tries, equalTo(4))
        assertThat(dao.nextToFlush()?.id, equalTo("a"))
    }

    @Test
    fun resetSkippedShouldDoNothingOnEmptyTable() {
        dao.resetSkipped()

        assertThat(dao.count(), equalTo(0))
    }
}

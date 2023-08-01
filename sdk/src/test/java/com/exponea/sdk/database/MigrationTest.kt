package com.exponea.sdk.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.DatabaseStorageObject
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.ExportedEventRoom
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.Route
import com.exponea.sdk.models.Route.TRACK_EVENTS
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.repository.EventRepositoryImpl
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.close
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MigrationTest : ExponeaSDKTest() {

    private lateinit var roomDb: ExponeaRoomDatabase
    private lateinit var paperDb: PaperDatabase
    private lateinit var repo: EventRepository

    private fun setup(paperDbEventCount: Int = 0, roomDbEventCount: Int = 0) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        paperDb = PaperDatabase(context, "EventDatabase")
        roomDb = Room.databaseBuilder(
            context,
            ExponeaRoomDatabase::class.java, "ExponeaEventDatabase"
        ).run {
            this.enableMultiInstanceInvalidation()
                .allowMainThreadQueries()
                .build()
        }
        repo = EventRepositoryImpl(context, ExponeaPreferencesImpl(context))
        paperDb.clear()
        roomDb.clear()
        repo.clear()
        createEventsInPaperDB(paperDbEventCount)
        createEventsInRoomDB(roomDbEventCount, paperDbEventCount)
    }

    private fun createEventsInPaperDB(eventCount: Int) {
        for (i in 1..eventCount) {
            paperDb.add(
                DatabaseStorageObject(
                    id = i.toString(),
                    projectId = "old-project-id",
                    item = ExportedEventType(
                        type = "test_event_$i",
                        timestamp = System.currentTimeMillis() / 1000.0,
                        customerIds = hashMapOf(Pair("first name $i", "second name $i")),
                        properties = hashMapOf("property" to i),
                        age = i.div(1.234)
                    ),
                    tries = i,
                    route = Route.TRACK_EVENTS,
                    exponeaProject = ExponeaProject(
                        "mock_base_url.com",
                        "mock_project_token",
                        "mock_auth"
                    )
                )
            )
        }
    }

    private fun createEventsInRoomDB(eventCount: Int, indexShift: Int = 0) {
        for (i in (1 + indexShift)..(eventCount + indexShift)) {
            roomDb.add(ExportedEventRoom(
                id = i.toString(),
                tries = i,
                projectId = "old-project-id",
                route = TRACK_EVENTS,
                shouldBeSkipped = false,
                exponeaProject = ExponeaProject(
                    "mock_base_url.com",
                    "mock_project_token",
                    "mock_auth"
                ),
                type = "test_event_$i",
                timestamp = System.currentTimeMillis() / 1000.0,
                age = i.div(1.234),
                customerIds = hashMapOf(Pair("first name $i", "second name $i")),
                properties = hashMapOf("property" to i)
            ))
        }
    }

    @Test
    fun `should migrate all events from PaperDB`() {
        setup(100)
        assertEquals(repo.count(), 0)
        repo.tryToMigrate()
        assertEquals(repo.count(), 100)
        for (i in 1..100) {
            val event = repo.get(i.toString())
            assertEquals(event?.projectId, "old-project-id")
            assertEquals(event?.type, "test_event_$i")
            assertEquals(event?.customerIds?.get("first name $i"), "second name $i")
            assertEquals(event?.properties?.get("property"), i)
            assertEquals(event?.route, Route.TRACK_EVENTS)
            assertEquals(event?.exponeaProject, ExponeaProject(
                "mock_base_url.com",
                "mock_project_token",
                "mock_auth"
            ))
            assertEquals(event?.tries, i)
            assertEquals(event?.age, i.div(1.234))
        }
    }

    @Test
    fun `should migrate all events from RoomDB`() {
        setup(roomDbEventCount = 100)
        assertEquals(repo.count(), 0)
        repo.tryToMigrate()
        assertEquals(repo.count(), 100)
        for (i in 1..100) {
            val event = repo.get(i.toString())
            assertEquals(event?.projectId, "old-project-id")
            assertEquals(event?.type, "test_event_$i")
            assertEquals(event?.customerIds?.get("first name $i"), "second name $i")
            assertEquals(event?.properties?.get("property"), i.toDouble())
            assertEquals(event?.route, Route.TRACK_EVENTS)
            assertEquals(event?.exponeaProject, ExponeaProject(
                "mock_base_url.com",
                "mock_project_token",
                "mock_auth"
            ))
            assertEquals(event?.tries, i)
            assertEquals(event?.age, i.div(1.234))
        }
    }

    @Test
    fun `should migrate only once from PaperDB`() {
        setup(20)
        assertEquals(repo.count(), 0)
        repo.tryToMigrate()
        assertEquals(repo.count(), 20)
        createEventsInPaperDB(10)
        repo.tryToMigrate()
        assertEquals(repo.count(), 20)
    }

    @Test
    fun `should migrate only once from RoomDB`() {
        setup(roomDbEventCount = 20)
        assertEquals(repo.count(), 0)
        repo.tryToMigrate()
        assertEquals(repo.count(), 20)
        createEventsInPaperDB(10)
        repo.tryToMigrate()
        assertEquals(repo.count(), 20)
    }

    @Test
    fun `should migrate only once from Paper and RoomDB`() {
        setup(paperDbEventCount = 20, roomDbEventCount = 20)
        assertEquals(repo.count(), 0)
        repo.tryToMigrate()
        assertEquals(repo.count(), 40)
        createEventsInPaperDB(10)
        repo.tryToMigrate()
        assertEquals(repo.count(), 40)
        createEventsInRoomDB(10, 40)
        repo.tryToMigrate()
        assertEquals(repo.count(), 40)
    }

    @Test
    fun `should migrate when sdk is initialized`() {
        setup(20, 20)
        assertEquals(repo.count(), 0)
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(ApplicationProvider.getApplicationContext(), ExponeaConfiguration())
        // migrated db should contain previous evens + install event
        assertEquals(41, repo.count())
    }

    @After
    fun closeDB() {
        roomDb.close()
        (repo as EventRepositoryImpl).close()
    }
}

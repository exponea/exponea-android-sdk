package com.exponea.sdk.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.DatabaseStorageObject
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.Route
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

    private lateinit var paperDb: PaperDatabase
    private lateinit var repo: EventRepository

    private fun setup(eventCount: Int) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        paperDb = PaperDatabase(context, "EventDatabase")
        repo = EventRepositoryImpl(context, ExponeaPreferencesImpl(context))
        paperDb.clear()
        repo.clear()
        createEvents(eventCount)
    }

    private fun createEvents(eventCount: Int) {
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
                        "mock-project-token",
                        "mock_auth"
                    )
                )
            )
        }
    }

    @Test
    fun `should migrate all events`() {
        setup(100)
        assertEquals(repo.count(), 0)
        repo.tryToMigrateFromPaper()
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
                "mock-project-token",
                "mock_auth"
            ))
            assertEquals(event?.tries, i)
            assertEquals(event?.age, i.div(1.234))
        }
    }

    @Test
    fun `should migrate only once`() {
        setup(20)
        assertEquals(repo.count(), 0)
        repo.tryToMigrateFromPaper()
        assertEquals(repo.count(), 20)
        createEvents(10)
        repo.tryToMigrateFromPaper()
        assertEquals(repo.count(), 20)
    }

    @Test
    fun `should migrate when sdk is initialized`() {
        setup(20)
        assertEquals(repo.count(), 0)
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(ApplicationProvider.getApplicationContext(), ExponeaConfiguration(projectToken = "mock-token"))
        // migrated db should contain previous evens + install event
        assertEquals(repo.count(), 21)
    }

    @After
    fun closeDB() {
        (repo as EventRepositoryImpl).close()
    }
}

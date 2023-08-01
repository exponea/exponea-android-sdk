package com.exponea.sdk.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.ExportedEventRoom
import com.exponea.sdk.models.Route
import com.exponea.sdk.models.Route.TRACK_EVENTS
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.waitForIt
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExponeaRoomDatabaseTest : ExponeaSDKTest() {

    companion object {
        const val DB_NAME = "TestDatabase"
    }

    private lateinit var db: ExponeaRoomDatabase
    private val mockData = ExportedEventRoom(
        properties = hashMapOf(Pair("key", "value")),
        age = 1.234,
        projectId = "mock_project_id",
        route = TRACK_EVENTS,
        exponeaProject = ExponeaProject("mock_base_url.com", "mock_project_token", "mock_auth")
    )

    @Before
    fun init() {
        db = Room.databaseBuilder(
                ApplicationProvider.getApplicationContext(),
                ExponeaRoomDatabase::class.java, DB_NAME
        ).enableMultiInstanceInvalidation()
        .allowMainThreadQueries().build()
    }

    @Test
    fun `should add item with correct count`() {
        assertEquals(db.count(), 0)
        db.add(mockData)
        assertEquals(db.count(), 1)
    }

    @Test
    fun `should get item`() {
        db.add(mockData)
        db.get(mockData.id)?.let {
            assertEquals("value", it.properties?.get("key"))
        }
    }

    @Test
    fun `should update item`() {
        db.add(mockData)
        mockData.age = 2.345
        db.update(item = mockData)
        db.get(mockData.id)?.let {
            assertEquals(2.345, it.age)
        }
    }

    @Test
    fun `should remove item`() {
        db.add(mockData)
        db.remove(mockData.id)
        val item = db.get(mockData.id)
        assertTrue { item == null }
    }

    @Test
    fun `should add items from multiple threads`() {
        waitForIt {
            val threadCount = 10
            var done = 0
            for (i in 1..threadCount) {
                thread {
                    for (x in 1..10) {
                        db.add(
                            ExportedEventRoom(
                                customerIds = hashMapOf(Pair("first name $i $x", "second name")),
                                projectId = "mock_project_id",
                                route = TRACK_EVENTS,
                                exponeaProject = ExponeaProject("mock_base_url.com", "mock_project_token", "mock_auth")
                            )
                        )
                    }
                    done++
                    if (done == threadCount) it()
                }
            }
        }
        assertEquals(100, db.all().size)
    }

    @Test
    fun `should not delete anything on empty id`() {
        db.add(mockData)
        assertEquals(db.count(), 1)
        db.remove("")
        assertEquals(db.count(), 1)
    }

    @Test
    fun denit() {
        for (x in 1..10) {
            db.add(
                    ExportedEventRoom(
                            customerIds = hashMapOf(Pair("first name $x", "second name")),
                            projectId = "mock_project_id",
                            route = Route.TRACK_EVENTS,
                            exponeaProject = ExponeaProject("mock_base_url.com", "mock_project_token", "mock_auth")
                    )
            )
        }
        db.clear()
        assertEquals(db.count(), 0)
        assertTrue(db.all().isEmpty())
    }

    @After
    fun closeDB() {
        denit()
        db.openHelper.close()
    }
}

package com.exponea.sdk

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.database.ExponeaDatabase
import com.exponea.sdk.database.ExponeaDatabaseImpl
import com.exponea.sdk.models.DatabaseStorageObject
import com.exponea.sdk.models.Route
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
internal class ExponeaDatabaseTest : ExponeaSDKTest() {

    companion object {
        const val DB_NAME = "TestDatabase"
        const val MOCK_PROJECT_ID = "projectId"
    }

    class Person(
        var firstName: String,
        var lastName: String
    )

    private lateinit var db: ExponeaDatabase<DatabaseStorageObject<Person>>
    private val mockData = DatabaseStorageObject(
        item = Person("first name", "second name"),
        projectId = MOCK_PROJECT_ID,
        route = Route.TRACK_EVENTS
    )

    @Before
    fun init() {
        db = ExponeaDatabaseImpl(ApplicationProvider.getApplicationContext(), DB_NAME)
    }

    @Test
    fun `should add item`() {
        assertEquals(true, db.add(mockData))
    }

    @Test
    fun `should get item`() {
        db.add(mockData)
        db.get(mockData.id)?.let {
            assertEquals("first name", it.item.firstName)
            assertEquals("second name", it.item.lastName)
        }
    }

    @Test
    fun `should update item`() {
        db.add(mockData)
        mockData.item.firstName = "anotherFirstName"
        db.update(item = mockData)
        db.get(mockData.id)?.let {
            assertEquals("anotherFirstName", it.item.firstName)
        }
    }

    @Test
    fun `should remove item`() {
        assertEquals(true, db.add(mockData))
        assertEquals(true, db.remove(mockData.id))
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
                            DatabaseStorageObject(
                                item = Person("first name $i $x", "second name"),
                                projectId = MOCK_PROJECT_ID,
                                route = Route.TRACK_EVENTS
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

    @After
    @Test
    fun denit() {
        assertEquals(true, db.clear())
    }
}

package com.exponea.sdk

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.database.ExponeaDatabase
import com.exponea.sdk.database.ExponeaDatabaseImpl
import com.exponea.sdk.models.DatabaseStorageObject
import com.exponea.sdk.models.Route
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.paperdb.Paper
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
internal class ExponeaDatabaseTest: ExponeaSDKTest() {

    companion object {
        const val DB_NAME = "TestDatabase"
        const val MOCK_PROJECT_ID = "projectId"
    }

    class Person(
            var firstName: String,
            var lastName: String
    )

    private lateinit var db : ExponeaDatabase<DatabaseStorageObject<Person>>
    private val mockData = DatabaseStorageObject(item = Person("firstname", "secondname"), projectId = MOCK_PROJECT_ID,
            route = Route.TRACK_EVENTS)

    @Before
    fun init() {
        Paper.init(ApplicationProvider.getApplicationContext())
        db = ExponeaDatabaseImpl(DB_NAME)
    }

    @Test
    fun testAdd_ShouldPass() {
        assertEquals(true, db.add(mockData))
    }

    @Test
    fun testGet_ShouldPass() {
        val success = db.add(mockData)
        if (success) {
            db.get(mockData.id)?.let {
                assertEquals("firstname",it.item.firstName)
            }
        }
    }

    @Test
    fun testUpdate_ShouldPass() {
        db.add(mockData)
        mockData.item.firstName = "anotherFirstName"
        db.update(item = mockData)
        db.get(mockData.id)?.let {
            assertEquals("anotherFirstName", it.item.firstName)
        }
    }

    @Test
    fun testRemove_ShouldPass() {
        assertEquals(true,db.add(mockData))
        assertEquals(true,db.remove(mockData.id))
        val item = db.get(mockData.id)
        assertTrue { item == null }
    }

    @After
    @Test
    fun denit() {
        assertEquals(true,db.clear())
    }

}

package com.exponea.sdk

import com.exponea.sdk.database.ExponeaDatabase
import com.exponea.sdk.database.ExponeaDatabaseImpl
import com.exponea.sdk.models.DatabaseStorageObject
import io.paperdb.Paper
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ExponeaDatabaseTest {

    companion object {
        const val DB_NAME = "TestDatabase"
        const val MOCK_PROJECT_ID = "projectId"
    }

    class Person(
            var firstName: String,
            var lastName: String
    )

    lateinit var db : ExponeaDatabase<DatabaseStorageObject<Person>>
    val mockData = DatabaseStorageObject(item = Person("firstname", "secondname"), projectId = MOCK_PROJECT_ID)

    @Before
    fun init() {
        Paper.init(RuntimeEnvironment.application.applicationContext)
        db = ExponeaDatabaseImpl(DB_NAME)
    }

    @Test
    fun testAdd_ShouldPass() {
        assertEquals(true, db.add(mockData))
    }

    @Test
    fun testGet_ShouldPass() {
        db.add(mockData)
        assertEquals("firstname", db.get(mockData.id).item.firstName)
    }

    @Test
    fun testUpdate_ShouldPass() {
        db.add(mockData)
        mockData.item.firstName = "anotherFirstName"
        db.update(item = mockData)
        assertEquals("anotherFirstName", db.get(mockData.id).item.firstName)
    }

    @Test
    fun testRemove_ShouldPass() {
        assertEquals(true,db.add(mockData))
        assertEquals(true,db.remove(mockData.id))
        try {
            db.get(mockData.id)
        } catch (ex: Exception) {
            assert(ex is IllegalStateException)
        }
        assertEquals(true, db.remove(mockData.id))
    }

    @After
    @Test
    fun denit() {
        assertEquals(true,db.clear())
    }



}
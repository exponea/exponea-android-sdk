package com.exponea.sdk.database

import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.ExportedEventRealm
import com.exponea.sdk.models.Route
import com.exponea.sdk.models.Route.TRACK_EVENTS
import com.exponea.sdk.models.toExportedEvent
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.UpdatePolicy.ALL
import io.realm.kotlin.ext.query
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.concurrent.thread
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test has to be created as Instrumental test because Realm is not supporting the JVM yet.
 * JVM could be done on latest 1.10.2 version but with issues
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
internal class ExponeaDatabaseRealmTest {

    private lateinit var db: Realm
    private val mockData = createRecord(
        hashMapOf(Pair("key", "value")),
        1.234,
        "mock_project_id",
        TRACK_EVENTS,
        ExponeaProject("mock_base_url.com", "mock_project_token", "mock_auth")
    )

    private fun createRecord(
        properties: HashMap<String, Any>? = null,
        age: Double? = null,
        projectId: String? = null,
        route: Route? = null,
        exponeaProject: ExponeaProject? = null,
        customerIds: HashMap<String, String?>? = null
    ): ExportedEventRealm {
        return ExportedEventRealm().apply {
            this.properties = Converters.instance.fromAnyMap(properties)
            this.age = age
            this.projectId = projectId
            this.route = Converters.instance.fromRoute(route)
            this.exponeaProject = Converters.instance.fromProject(
                exponeaProject
            )
            this.customerIds = Converters.instance.fromStringMap(customerIds)
        }
    }

    @Before
    fun init() {
        db = Realm.open(
            RealmConfiguration.Builder(setOf(ExportedEventRealm::class))
                .inMemory()
                .build()
        )
    }

    @Test
    fun shouldAddItemWithCorrectCount() {
        assertEquals(db.query<ExportedEventRealm>().count().find(), 0)
        db.writeBlocking {
            copyToRealm(createRecord(
                hashMapOf(Pair("key", "value")),
                1.234,
                "mock_project_id",
                TRACK_EVENTS,
                ExponeaProject("mock_base_url.com", "mock_project_token", "mock_auth")
            ), updatePolicy = ALL)
        }
        assertEquals(db.query<ExportedEventRealm>().count().find(), 1)
    }

    @Test
    fun shouldGetItem() {
        db.writeBlocking {
            copyToRealm(createRecord(
                hashMapOf(Pair("key", "value")),
                1.234,
                "mock_project_id",
                TRACK_EVENTS,
                ExponeaProject("mock_base_url.com", "mock_project_token", "mock_auth")
            ), updatePolicy = ALL)
        }
        db.query<ExportedEventRealm>("id == $0", mockData.id).first().find()?.let {
            assertEquals("value", it.toExportedEvent().properties?.get("key"))
        }
    }

    @Test
    fun shouldUpdateItem() {
        db.writeBlocking {
            copyToRealm(mockData, updatePolicy = ALL)
        }
        mockData.age = 2.345
        db.writeBlocking {
            copyToRealm(mockData, updatePolicy = ALL)
        }
        db.query<ExportedEventRealm>("id == $0", mockData.id).first().find()?.let {
            assertEquals(2.345, it.age)
        }
    }

    @Test
    fun shouldRemoveItem() {
        db.writeBlocking {
            copyToRealm(mockData, updatePolicy = ALL)
        }
        db.writeBlocking {
            query<ExportedEventRealm>("id == $0", mockData.id).first().find()?.let {
                delete(it)
            }
        }
        val item = db.query<ExportedEventRealm>("id == $0", mockData.id).first().find()
        assertNull(item)
    }

    @Test
    fun shouldAddItemsFromMultipleThreads() {
        val gate = CountDownLatch(1)
        val threadCount = 10
        var done = 0
        for (i in 1..threadCount) {
            thread {
                for (x in 1..10) {
                    db.writeBlocking {
                        copyToRealm(createRecord(
                            customerIds = hashMapOf(Pair("first name $i $x", "second name")),
                            projectId = "mock_project_id",
                            route = TRACK_EVENTS,
                            exponeaProject = ExponeaProject("mock_base_url.com", "mock_project_token", "mock_auth")
                        ), updatePolicy = ALL)
                    }
                }
                done++
                if (done == threadCount) gate.countDown()
            }
        }
        assertTrue(gate.await(20, SECONDS))
        assertEquals(100, db.query<ExportedEventRealm>().find().size)
    }

    @Test
    fun shouldNotDeleteAnythingOnEmptyId() {
        db.writeBlocking {
            copyToRealm(mockData, updatePolicy = ALL)
        }
        assertEquals(db.query<ExportedEventRealm>().count().find(), 1)
        db.writeBlocking {
            query<ExportedEventRealm>("id == $0", "").first().find()?.let {
                delete(it)
            }
        }
        assertEquals(db.query<ExportedEventRealm>().count().find(), 1)
    }

    @Test
    fun denit() {
        for (x in 1..10) {
            db.writeBlocking {
                copyToRealm(createRecord(
                    customerIds = hashMapOf(Pair("first name $x", "second name")),
                    projectId = "mock_project_id",
                    route = Route.TRACK_EVENTS,
                    exponeaProject = ExponeaProject(
                        "mock_base_url.com",
                        "mock_project_token",
                        "mock_auth"
                    )
                ), updatePolicy = ALL)
            }
        }
        db.writeBlocking {
            this.delete(ExportedEventRealm::class)
        }
        assertEquals(db.query<ExportedEventRealm>().count().find(), 0)
        assertTrue(db.query<ExportedEventRealm>().find().isEmpty())
    }

    @After
    fun closeDB() {
        db.writeBlocking {
            this.delete(ExportedEventRealm::class)
        }
        db.close()
    }
}

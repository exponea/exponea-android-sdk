package com.exponea.sdk.repository

import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.Route
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.testutil.close
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit.MINUTES
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test has to be created as Instrumental test because Realm is not supporting the JVM yet.
 * JVM could be done on latest 1.10.2 version but with issues
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class EventRepositoryImplTest {

    private lateinit var repo: EventRepository

    @Before
    fun init() {
        val context = InstrumentationRegistry.getInstrumentation().context
        repo = EventRepositoryImpl(context, ExponeaPreferencesImpl(context))
        repo.clear()
    }

    @After
    fun closeDB() {
        (repo as EventRepositoryImpl).close()
    }

    private fun createTestEvent(type: String? = "test_event"): ExportedEvent {
        val event = ExportedEvent(
            projectId = "old-project-id",
            type = type,
            timestamp = System.currentTimeMillis() / 1000.0,
            customerIds = hashMapOf(),
            properties = hashMapOf("property" to "value"),
            route = Route.TRACK_EVENTS,
            exponeaProject = ExponeaProject(
                "mock_base_url.com",
                "mock_project_token",
                "mock_auth"
            )
        )
        repo.add(event)
        return event
    }

    @Test
    fun testMemoryRequirementsForTest() {
        val runtime = Runtime.getRuntime()
        val usedMemInMB = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
        val maxHeapSizeInMB = runtime.maxMemory() / 1048576L
        val availHeapSizeInMB = maxHeapSizeInMB - usedMemInMB
        // found by try-fail and depends on `threadsCount` in `shouldBeThreadSafe`
        val expectedMinimalMemoryMB = 120
        assertTrue(
            "This test needs more than $expectedMinimalMemoryMB MB but got $availHeapSizeInMB MB",
            availHeapSizeInMB >= expectedMinimalMemoryMB
        )
    }

    @Test
    fun shouldBeThreadSafe() {
        val readCount = 100
        val createCount = 100
        val updateCount = 100
        val deleteCount = 100
        val getAllCount = 100
        val threadsCount = readCount + updateCount + deleteCount + getAllCount + createCount
        val recordsToRead = mutableListOf<ExportedEvent>()
        for (i in 0 until readCount) {
            recordsToRead.add(createTestEvent(type = Constants.EventTypes.push))
        }
        val recordsToUpdate = mutableListOf<ExportedEvent>()
        for (i in 0 until updateCount) {
            recordsToUpdate.add(createTestEvent(type = Constants.EventTypes.push))
        }
        val recordsToDelete = mutableListOf<ExportedEvent>()
        for (i in 0 until deleteCount) {
            recordsToDelete.add(createTestEvent(type = Constants.EventTypes.push))
        }
        val service: ExecutorService = Executors.newFixedThreadPool(threadsCount)
        val startPoint = Semaphore(0)
        val doneTaskCount = CountDownLatch(threadsCount)
        for (each in recordsToRead) {
            service.submit {
                startPoint.acquire()
                assertNotNull(repo.get(each.id))
                doneTaskCount.countDown()
            }
        }
        for (i in 0 until createCount) {
            service.submit {
                startPoint.acquire()
                createTestEvent(type = Constants.EventTypes.push)
                doneTaskCount.countDown()
            }
        }
        for (each in recordsToUpdate) {
            service.submit {
                startPoint.acquire()
                each.age = 10.0
                repo.update(each)
                doneTaskCount.countDown()
            }
        }
        for (each in recordsToDelete) {
            service.submit {
                startPoint.acquire()
                repo.remove(each.id)
                doneTaskCount.countDown()
            }
        }
        for (i in 0 until getAllCount) {
            service.submit {
                startPoint.acquire()
                assertNotNull(repo.all())
                doneTaskCount.countDown()
            }
        }
        // start all DB actions at once
        startPoint.release(threadsCount)
        // wait until all actions are done (could be slow on low-end)
        val isDone = doneTaskCount.await(2, MINUTES)
        if (!isDone) {
            fail("Parallel tasks are not done, still waiting for ${doneTaskCount.count} of them")
        }
        assertEquals(300, repo.all().size)
    }
}

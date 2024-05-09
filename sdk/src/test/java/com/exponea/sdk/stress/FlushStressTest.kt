package com.exponea.sdk.stress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.ConnectionManager
import com.exponea.sdk.manager.FlushManager
import com.exponea.sdk.manager.FlushManagerImpl
import com.exponea.sdk.manager.InAppMessageManagerImpl
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.DeviceProperties
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.repository.EventRepositoryImpl
import com.exponea.sdk.testutil.ExponeaMockServer
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.componentForTesting
import com.exponea.sdk.testutil.mocks.ExponeaMockService
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.currentTimeSeconds
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.util.Random
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FlushStressTest : ExponeaSDKTest() {
    companion object {
        val configuration = ExponeaConfiguration()
        val server = ExponeaMockServer.createServer()
        const val stressCount = 500

        @BeforeClass
        @JvmStatic
        fun setup() {
            configuration.baseURL = server.url("/").toString()
            configuration.projectToken = "projectToken"
            configuration.authorization = "Token projectAuthorization"
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            server.shutdown()
        }
    }

    private lateinit var properties: PropertiesList
    private lateinit var manager: FlushManager
    private lateinit var repo: EventRepository
    private lateinit var service: ExponeaMockService

    @Before
    fun init() {
        mockkConstructorFix(EventRepositoryImpl::class)
        mockkConstructorFix(InAppMessageManagerImpl::class)
        every {
            anyConstructed<InAppMessageManagerImpl>().show(any())
        } just Runs
        val context = ApplicationProvider.getApplicationContext<Context>()
        properties = PropertiesList(properties = DeviceProperties(context).toHashMap())
        val connectedManager = mockk<ConnectionManager>()
        every { connectedManager.isConnectedToInternet() } returns true
        skipInstallEvent()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)

        repo = Exponea.componentForTesting.eventRepository
        service = ExponeaMockService(true)
        manager = FlushManagerImpl(configuration, repo, service, connectedManager, {})
        repo.clear()
    }

    @Test
    fun testFlushingStressed() {
        val semaphore = Semaphore(0)
        every { repo.add(any()) } answers {
            callOriginal()
            semaphore.release()
        }
        val r = Random()
        var insertedCount = 0
        for (i in 0 until stressCount) {

            ExponeaMockServer.setResponseSuccess(server, "tracking/track_event_success.json")

            val eventType = when {
                i % 7 == 0 -> Constants.EventTypes.sessionEnd
                i % 5 == 0 -> Constants.EventTypes.installation
                i % 3 == 0 -> Constants.EventTypes.sessionStart
                i % 2 == 0 -> Constants.EventTypes.payment
                else -> Constants.EventTypes.push
            }
            if (repo.all().size != insertedCount) {
                assertEquals(repo.all().size, insertedCount)
            }
            Exponea.trackEvent(
                eventType = eventType,
                timestamp = currentTimeSeconds(),
                properties = properties
            )
            // wait for event to be inserted
            assertTrue(semaphore.tryAcquire(2, TimeUnit.SECONDS))
            insertedCount++
            if (r.nextInt(10) == 3) {
                if (repo.all().size != insertedCount) {
                    assertEquals(repo.all().size, insertedCount)
                }
                insertedCount = 0
                waitForIt {
                    manager.flushData { _ ->
                        it.assertEquals(0, repo.all().size)
                        it()
                    }
                }
            }
        }
    }
}

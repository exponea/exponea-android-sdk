package com.exponea.sdk.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.testutil.ExponeaSDKTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class InstallEventTest : ExponeaSDKTest() {

    companion object {
        val configuration = ExponeaConfiguration()
        val server = MockWebServer()

        @BeforeClass
        @JvmStatic
        fun setup() {
            configuration.projectToken = "TestTokem"
            configuration.authorization = "TestTokenAuthentication"
            configuration.baseURL = server.url("").toString().substringBeforeLast("/")
            println(configuration.baseURL)
            configuration.maxTries = 10
        }

        @AfterClass
        fun tearDown() {
            server.shutdown()
        }
    }

    lateinit var repo: EventRepository

    @Before
    fun prepareForTest() {

        val context = ApplicationProvider.getApplicationContext<Context>()

        // First Install event success
        ExponeaMockServer.setResponseSuccess(server, "tracking/track_event_success.json")

        Exponea.init(context, configuration)
        Exponea.flushMode = FlushMode.MANUAL

        repo = Exponea.component.eventRepository

        // Clean event repository for testing purposes
        repo.clear()


    }

    @Test
    fun testInstallEventAdded_ShouldSuccess() {

        Exponea.component.deviceInitiatedRepository.set(false)
        Exponea.trackInstallEvent()

        // The only event tracked by now should be install_event
        val event = repo.all().first()
        assertEquals(Constants.EventTypes.installation, event.item.type)
    }

    @Test
    fun sendInstallEvenTest_ShouldPass() {

        Exponea.component.deviceInitiatedRepository.set(false)
        Exponea.trackInstallEvent()

        ExponeaMockServer.setResponseSuccess(server, "tracking/track_event_success.json")

        val lock = CountDownLatch(1)

        Exponea.flushData()

        Exponea.component.flushManager.onFlushFinishListener = {
            assertEquals(0, Exponea.component.eventRepository.all().size)
            lock.countDown()
        }
        lock.await()

        val request = server.takeRequest(5, TimeUnit.SECONDS)

        assertEquals("/track/v2/projects/TestTokem/customers/events", request.path)
        assertEquals(request.getHeader("Authorization"), "TestTokenAuthentication")
    }
}
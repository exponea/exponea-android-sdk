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
import com.exponea.sdk.testutil.componentForTesting
import com.exponea.sdk.testutil.waitForIt
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import okhttp3.mockwebserver.MockWebServer
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InstallEventTest : ExponeaSDKTest() {

    companion object {
        val configuration = ExponeaConfiguration()
        lateinit var server: MockWebServer

        @BeforeClass
        @JvmStatic
        fun setup() {
            server = ExponeaMockServer.createServer()
            configuration.projectToken = "TestTokem"
            configuration.authorization = "TestTokenAuthentication"
            configuration.baseURL = server.url("").toString().substringBeforeLast("/")
            configuration.maxTries = 10
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            server.shutdown()
        }
    }

    private lateinit var repo: EventRepository

    @Before
    fun prepareForTest() {

        val context = ApplicationProvider.getApplicationContext<Context>()

        skipInstallEvent()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)

        repo = Exponea.componentForTesting.eventRepository
    }

    @Test
    fun testInstallEventAdded_ShouldSuccess() {

        Exponea.componentForTesting.deviceInitiatedRepository.set(false)
        Exponea.trackInstallEvent()

        // The only event tracked by now should be install_event
        val event = repo.all().first()
        assertEquals(Constants.EventTypes.installation, event.item.type)
    }

    @Test
    fun sendInstallEvenTest_ShouldPass() {

        Exponea.componentForTesting.deviceInitiatedRepository.set(false)
        Exponea.trackInstallEvent()

        ExponeaMockServer.setResponseSuccess(server, "tracking/track_event_success.json")

        waitForIt {
            Exponea.flushData { _ ->
                it.assertEquals(0, Exponea.componentForTesting.eventRepository.all().size)
                it()
            }
        }

        val request = server.takeRequest(5, TimeUnit.SECONDS)

        assertEquals("/track/v2/projects/TestTokem/customers/events", request.path)
        assertEquals(request.getHeader("Authorization"), "TestTokenAuthentication")
    }
}

package com.exponea.sdk.tracking

import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.experimental.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment


@RunWith(RobolectricTestRunner::class)
class InstallEventTest {
    companion object {
        var hasSetup: Boolean = false
    }

    val mockServer = MockWebServer()

    @Before
    public fun setup() {
        val mockServerAddress = setupWebServer()

        println("mockServerAddress: $mockServerAddress")

        val context = RuntimeEnvironment.application

        val configuration = ExponeaConfiguration()
        configuration.baseURL = mockServerAddress
        configuration.projectToken = "projectToken"
        configuration.authorization = "projectAuthorization"

        Exponea.init(context, configuration)

        Exponea.flushMode = FlushMode.MANUAL

        hasSetup = true
    }

    @After
    public fun tearDown() {
        //mockServer.shutdown()
    }

    @Test
    fun testInstallEventAdded() {

        // The only event tracked by now should be install_event
        val event = Exponea.component.eventRepository.all().first()
        assertEquals(Constants.EventTypes.installation, event.item.type)

        // No more than 1 install event should be tracked
        Exponea.trackInstall()
        assertEquals(1, Exponea.component.eventRepository.all().size)

    }

    private fun setupWebServer(): String {
        mockServer.start()
        return mockServer.url("/").toString()
    }

    @Test
    fun sendInstallEvenTest_ShouldPass() {
        val mockResponse = MockResponse()
                .setBody("")
                .setResponseCode(200)

        mockServer.enqueue(mockResponse)

        Exponea.trackInstall()
        runBlocking {
            Exponea.flush()
            Exponea.component.flushManager.onFlushFinishListener = {
                assertEquals(0, Exponea.component.eventRepository.all().size)
            }
        }

        val request = mockServer.takeRequest()
        assertEquals("/track/v2/projects/projectToken/customers/events", request.path)
        assertEquals(request.getHeader("Authorization"), "projectAuthorization")
    }
}
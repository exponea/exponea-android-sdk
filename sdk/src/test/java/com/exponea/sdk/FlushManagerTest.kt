package com.exponea.sdk

import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.manager.FlushManager
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.Payment
import com.exponea.sdk.repository.EventRepository
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class FlushManagerTest {

    lateinit var manager: FlushManager
    lateinit var repo: EventRepository

    @Before
    fun init() {
        ExponeaMockServer.setUp()

        val context = RuntimeEnvironment.application

        val configuration = ExponeaConfiguration()
        configuration.baseURL = ExponeaMockServer.address
        configuration.projectToken = "projectToken"
        configuration.authorization = "projectAuthorization"

        Exponea.init(context, configuration)
        Exponea.flushMode = FlushMode.MANUAL

        repo = Exponea.component.eventRepository
        manager = Exponea.component.flushManager

    }


    @Test
    fun flushEvents_ShouldPass() {
        ExponeaMockServer.setResponseSuccess("tracking/track_event_success.json")
        manager.flush()
        manager.onFlushFinishListener = {
            assertEquals(0, repo.all().size)
        }
    }

    @Test
    fun flushEvents_ShouldFail() {
        ExponeaMockServer.setResponseError("tracking/track_event_failed.json")
        manager.flush()
        manager.onFlushFinishListener = {
            assertEquals(1, repo.all().size)
        }
    }

}
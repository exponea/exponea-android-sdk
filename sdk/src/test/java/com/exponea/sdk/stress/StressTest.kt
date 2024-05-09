package com.exponea.sdk.stress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.DeviceProperties
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.testutil.ExponeaMockServer
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.componentForTesting
import com.exponea.sdk.testutil.runInSingleThread
import com.exponea.sdk.util.currentTimeSeconds
import java.util.Random
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class StressTest : ExponeaSDKTest() {

    companion object {
        val configuration = ExponeaConfiguration(automaticSessionTracking = false)
        val server = ExponeaMockServer.createServer()
        const val stressCount = 1000

        @BeforeClass
        @JvmStatic
        fun setup() {
            configuration.projectToken = "TestTokem"
            configuration.authorization = "Token TestTokenAuthentication"
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
    private lateinit var properties: PropertiesList

    @Before
    fun prepareForTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        properties = PropertiesList(properties = DeviceProperties(context).toHashMap())
        skipInstallEvent()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)
        repo = Exponea.componentForTesting.eventRepository
    }

    @Test
    fun testTrackEventStressed() = runInSingleThread { idleThreads ->

        val eventList = mutableListOf<String>()
        for (i in 0 until stressCount) {

            val eventType = when {
                i % 7 == 0 -> Constants.EventTypes.sessionEnd
                i % 5 == 0 -> Constants.EventTypes.installation
                i % 3 == 0 -> Constants.EventTypes.sessionStart
                i % 2 == 0 -> Constants.EventTypes.payment
                else -> Constants.EventTypes.push
            }
            eventList += eventType
            Exponea.trackEvent(
                eventType = eventType,
                timestamp = currentTimeSeconds(),
                properties = properties
            )
        }
        idleThreads()

        val installCount = eventList.filter { it == Constants.EventTypes.installation }.size
        val sessionEndCount = eventList.filter { it == Constants.EventTypes.sessionEnd }.size
        val sessionStartCount = eventList.filter { it == Constants.EventTypes.sessionStart }.size
        val paymentCount = eventList.filter { it == Constants.EventTypes.sessionStart }.size
        val pushCount = eventList.filter { it == Constants.EventTypes.push }.size

        var pushRepoCount = 0
        var paymentRepoCount = 0
        var sessionStartRepoCount = 0
        var sessionEndRepoCount = 0
        var installRepoCount = 0
        val unknownList = mutableListOf<ExportedEvent>()
        repo.all().forEach {
            when (it.type) {
                Constants.EventTypes.installation -> installRepoCount++
                Constants.EventTypes.sessionEnd -> sessionEndRepoCount++
                Constants.EventTypes.sessionStart -> sessionStartRepoCount++
                Constants.EventTypes.payment -> paymentRepoCount++
                Constants.EventTypes.push -> pushRepoCount++
                else -> unknownList += it
            }
        }

        assertEquals(installCount, installRepoCount)
        assertEquals(sessionEndCount, sessionEndRepoCount)
        assertEquals(sessionStartCount, sessionStartRepoCount)
        assertEquals(paymentCount, paymentRepoCount)
        assertEquals(pushCount, pushRepoCount)
        assertTrue(unknownList.isEmpty())
    }

    @Test
    fun testTrackCustomerStressed() = runInSingleThread { idleThreads ->
        // Track event
        for (i in 0 until stressCount) {
            Exponea.identifyCustomer(
                customerIds = CustomerIds().withId("registered", "john@doe.com"),
                properties = PropertiesList(hashMapOf("first_name" to "NewName"))
            )
        }
        idleThreads()
        assertEquals(stressCount, repo.all().size)
    }

    @ObsoleteCoroutinesApi
    @Test
    fun testTrackThreadsStressed() {
        val coroutineList = mutableListOf<CoroutineContext>()
        for (i in 0 until 5) {
            coroutineList += newSingleThreadContext(i.toString())
        }
        val r = Random()
        runBlocking {
            for (i in 0 until stressCount) {
                val eventType = when {
                    i % 7 == 0 -> Constants.EventTypes.sessionEnd
                    i % 5 == 0 -> Constants.EventTypes.installation
                    i % 3 == 0 -> Constants.EventTypes.sessionStart
                    i % 2 == 0 -> Constants.EventTypes.push
                    else -> Constants.EventTypes.payment
                }
                addEvent(
                    coroutineContext = coroutineList[r.nextInt(coroutineList.size)],
                    eventType = eventType,
                    timestamp = currentTimeSeconds(),
                    properties = properties
                )
            }
        }
        assertEquals(stressCount, repo.all().size)
    }

    private suspend fun addEvent(
        coroutineContext: CoroutineContext,
        properties: PropertiesList,
        timestamp: Double? = currentTimeSeconds(),
        eventType: String?
    ) {
        withContext(coroutineContext) {
            Exponea.trackEvent(
                eventType = eventType,
                timestamp = timestamp,
                properties = properties
            )
        }
    }
}

package com.exponea.sdk.tracking

import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.repository.CampaignRepository
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.util.currentTimeSeconds
import com.google.gson.GsonBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class CampaignClickEventTests : ExponeaSDKTest() {

    companion object {

        val configuration = ExponeaConfiguration()
        lateinit var server: MockWebServer

        @BeforeClass
        @JvmStatic
        fun setup() {
            server = MockWebServer()
            configuration.projectToken = "TestToken"
            configuration.authorization = "TestTokenAuthentication"
            configuration.baseURL = server.url("").toString().substringBeforeLast("/")
            configuration.maxTries = 10
            configuration.automaticSessionTracking = false
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            server.shutdown()
        }
    }

    private var context = InstrumentationRegistry.getInstrumentation().context
    private val UTM_SOURCE = "testSource"
    private val UTM_CAMPAIGN = "campaign001"
    private val UTM_CONTENT = "campaignTestContent"
    private val UTM_MEDIUM = "medium_98765rfghjmnb"
    private val UTM_TERM = "term_098765rtyuk"
    private val XNPE_CMP = "3456476768iu-ilkujyfgcvbi7gukgvbnp-oilgvjkjyhgdxcvbiu"
    private val CAMPAIGN_URL = "http://example.com/route/to/campaing" +
            "?utm_source=" + UTM_SOURCE +
            "&utm_campaign=" + UTM_CAMPAIGN +
            "&utm_content=" + UTM_CONTENT +
            "&utm_medium=" + UTM_MEDIUM +
            "&utm_term=" + UTM_TERM +
            "&xnpe_cmp=" + XNPE_CMP

    private lateinit var eventRepository: EventRepository
    private lateinit var campaignRepository: CampaignRepository

    @Before
    fun prepareForTest() {
        skipInstallEvent()
        Exponea.init(context, configuration)
        waitUntilFlushed()
        Exponea.flushMode = FlushMode.MANUAL

        eventRepository = Exponea.component.eventRepository
        campaignRepository = Exponea.component.campaignRepository

        eventRepository.clear()
        campaignRepository.clear()
    }

    @Test
    fun testHandleIntent_normal() {
        val deepLinkIntent = createDeeplinkIntent()
        assertTrue(Exponea.handleCampaignIntent(deepLinkIntent, context))

        val storedEvents = eventRepository.all()
        assertEquals(1, storedEvents.size)
        assertEquals(Constants.EventTypes.push, storedEvents.first().item.type)
    }

    @Test
    fun testHandleIntent_invalid_NullData() {
        val deepLinkIntent = createDeeplinkIntent()
        deepLinkIntent.data = null;
        val handled = Exponea.handleCampaignIntent(deepLinkIntent, context)
        val storedEvents = eventRepository.all()

        assertFalse { handled }
        assertEquals(0, storedEvents.size)
    }

    @Test
    fun testHandleIntent_invalid_InvalidUrl() {
        val deepLinkIntent = createDeeplinkIntent()
        deepLinkIntent.data = Uri.parse(CAMPAIGN_URL.replaceFirst("http", "invalid"));
        val handled = Exponea.handleCampaignIntent(deepLinkIntent, context)
        val storedEvents = eventRepository.all()

        assertFalse { handled }
        assertEquals(0, storedEvents.size)
    }

    @Test
    fun testHandleIntent_invalid_NoPayload() {
        val deepLinkIntent = createDeeplinkIntent()
        deepLinkIntent.data = Uri.parse(CAMPAIGN_URL.replaceFirst("xnpe_cmp", "x_xnpe_cmp"));
        val handled = Exponea.handleCampaignIntent(deepLinkIntent, context)
        val storedEvents = eventRepository.all()

        assertFalse { handled }
        assertEquals(0, storedEvents.size)
    }

    @Test
    fun testHandleIntent_invalid_IncompatibleAction() {
        val deepLinkIntent = createDeeplinkIntent()
        deepLinkIntent.action = Intent.ACTION_PASTE
        val handled = Exponea.handleCampaignIntent(deepLinkIntent, context)
        val storedEvents = eventRepository.all()

        assertFalse { handled }
        assertEquals(0, storedEvents.size)
    }

    @Test
    fun testHandleIntent_checkEventProperties() {
        val deepLinkIntent = createDeeplinkIntent()
        Exponea.handleCampaignIntent(deepLinkIntent, context)

        val storedEvent = eventRepository.all().first()

        assertTrue(storedEvent.item.properties!!.contains("timestamp"))
        assertTrue(storedEvent.item.properties!!.contains("platform"))
        assertTrue(storedEvent.item.properties!!.contains("url"))

        assertEquals(CAMPAIGN_URL, storedEvent.item.properties!!["url"])
        assertEquals("Android", storedEvent.item.properties!!["platform"])
    }

    @Test
    fun testHandleIntent_checkCampaignValues() {
        val deepLinkIntent = createDeeplinkIntent()
        Exponea.handleCampaignIntent(deepLinkIntent, context)

        val campaignEvent = campaignRepository.get()

        assertNotNull(campaignEvent)

        assertEquals(UTM_TERM, campaignEvent.term)
        assertEquals(UTM_CAMPAIGN, campaignEvent.campaign)
        assertEquals(UTM_CONTENT, campaignEvent.content)
        assertEquals(UTM_MEDIUM, campaignEvent.medium)
        assertEquals(XNPE_CMP, campaignEvent.payload)
        assertEquals(UTM_SOURCE, campaignEvent.source)
        assertEquals(CAMPAIGN_URL, campaignEvent.completeUrl)
        assertNotNull(campaignEvent.createdAt)

    }

    @Test
    fun testHandleIntent_oldCampaign() {
        val deepLinkIntent = createDeeplinkIntent()
        Exponea.handleCampaignIntent(deepLinkIntent, context)

        runBlocking {
            delay(Exponea.campaignTTL.plus(1).times(1000).toLong())
        }

        val campaignEvent = campaignRepository.get()

        assertNull(campaignEvent)

    }

    @Test
    fun testHandleIntent_sessionUpdate() {
        val deepLinkIntent = createDeeplinkIntent()
        Exponea.handleCampaignIntent(deepLinkIntent, context)
        val campaignEvent = campaignRepository.get()!!
        Exponea.trackSessionStart(currentTimeSeconds())

        val storedEvents = eventRepository.all()
        assertEquals(2, storedEvents.size)

        val sessionEvent = storedEvents.first {
            Constants.EventTypes.sessionStart == it.item.type
        }.item

        assertEquals(campaignEvent.completeUrl, sessionEvent.properties!!["location"])
        assertEquals(campaignEvent.source, sessionEvent.properties!!["utm_source"])
        assertEquals(campaignEvent.campaign, sessionEvent.properties!!["utm_campaign"])
        assertEquals(campaignEvent.content, sessionEvent.properties!!["utm_content"])
        assertEquals(campaignEvent.term, sessionEvent.properties!!["utm_term"])
    }

    @Test
    fun testHandleIntent_campaignFlush() {
        val deepLinkIntent = createDeeplinkIntent()
        Exponea.handleCampaignIntent(deepLinkIntent, context)

        val request = flushAndWaitForRequest()

        assertEquals("/track/v2/projects/TestToken/campaigns/clicks", request!!.path)
        var parsedRequest = GsonBuilder().create().fromJson(request.body.readUtf8(), Map::class.java) as Map<String, Any?>
        assertNotNull(parsedRequest["age"])
        assertNotNull(parsedRequest["properties"])
        assertEquals("Android", (parsedRequest["properties"] as Map<String, String>)["platform"])
        assertEquals(CAMPAIGN_URL, parsedRequest["url"])
    }

    private fun flushAndWaitForRequest(): RecordedRequest? {
        ExponeaMockServer.setResponseSuccess(server, "tracking/track_event_success.json")
        flushDataSync()
        return server.takeRequest(5, TimeUnit.SECONDS)
    }

    private fun flushDataSync() {
        val lock = CountDownLatch(1)
        Exponea.component.flushManager.onFlushFinishListener = {
            lock.countDown()
        }
        Exponea.flushData()
        lock.await()
    }

    private fun createDeeplinkIntent() = Intent().apply {
        this.action = Intent.ACTION_VIEW
        this.addCategory(Intent.CATEGORY_DEFAULT)
        this.addCategory(Intent.CATEGORY_BROWSABLE)
        this.data = Uri.parse(CAMPAIGN_URL)
    }

}

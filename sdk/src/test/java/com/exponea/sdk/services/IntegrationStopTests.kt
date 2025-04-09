package com.exponea.sdk.services

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.InAppContentBlockManagerImplTest
import com.exponea.sdk.manager.SessionManagerImpl
import com.exponea.sdk.models.CampaignData
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.models.Segment
import com.exponea.sdk.models.SegmentTest
import com.exponea.sdk.models.SegmentationDataCallback
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.AppInboxCacheImpl
import com.exponea.sdk.repository.AppInboxCacheImplTest
import com.exponea.sdk.repository.CampaignRepositoryImpl
import com.exponea.sdk.repository.CustomerIdsRepositoryImpl
import com.exponea.sdk.repository.DeviceInitiatedRepositoryImpl
import com.exponea.sdk.repository.DrawableCacheImpl
import com.exponea.sdk.repository.EventRepositoryImpl
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.repository.FontCacheImpl
import com.exponea.sdk.repository.HtmlNormalizedCacheImpl
import com.exponea.sdk.repository.InAppContentBlockDisplayStateRepositoryImpl
import com.exponea.sdk.repository.InAppMessageDisplayStateRepositoryImpl
import com.exponea.sdk.repository.InAppMessagesCacheImpl
import com.exponea.sdk.repository.PushTokenRepositoryProvider
import com.exponea.sdk.repository.SegmentsCacheImpl
import com.exponea.sdk.repository.UniqueIdentifierRepositoryImpl
import com.exponea.sdk.telemetry.TelemetryManager
import com.exponea.sdk.telemetry.TelemetryManager.Companion.INSTALL_ID_KEY
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.tracking.CampaignClickEventTests.Companion.CAMPAIGN_UNIVERSAL_LINK_URL
import com.exponea.sdk.util.ExponeaGson
import com.exponea.sdk.util.HtmlNormalizer
import com.exponea.sdk.util.TokenType
import java.util.Date
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IntegrationStopTests : ExponeaSDKTest() {

    @After
    fun resetSdkState() {
        Exponea.isStopped = false
    }

    @Test
    fun `Stop SDK before SDK init is denied`() {
        createSdkData()
        Exponea.isInitialized = false
        Exponea.stopIntegration()
        assertFalse(Exponea.isInitialized)
        assertFalse(Exponea.isStopped)
    }

    @Test
    fun `Stop SDK data after SDK init has to stop SDK and remove all data`() {
        createSdkData()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(ApplicationProvider.getApplicationContext(), ExponeaConfiguration(projectToken = "mock-token"))
        Exponea.stopIntegration()
        validateEmptySdkData()
    }

    @Test
    fun `Clearing local user data before SDK init has to remove local data`() {
        createSdkData()
        Exponea.isInitialized = false
        Exponea.clearLocalCustomerData()
        assertFalse(Exponea.isInitialized)
        assertFalse(Exponea.isStopped)
        validateEmptySdkData()
    }

    @Test
    fun `Clearing local user data after SDK init is denied`() {
        createSdkData()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(ApplicationProvider.getApplicationContext(), ExponeaConfiguration(projectToken = "mock-token"))
        Exponea.clearLocalCustomerData()
        assertTrue(Exponea.isInitialized)
        assertFalse(Exponea.isStopped)
        validateNonEmptySdkData()
    }

    private fun createSdkData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        EventRepositoryImpl(context).add(
            ExportedEvent(
                id = UUID.randomUUID().toString(),
                tries = 0,
                projectId = "projId",
                route = null,
                shouldBeSkipped = false
            )
        )
        AppInboxCacheImpl(context, ExponeaGson.instance).apply {
            setSyncToken("test token")
            addMessages(listOf(AppInboxCacheImplTest.buildMessage("id1")))
        }
        waitForIt {
            DrawableCacheImpl(context).preload(
                urls = listOf("https://upload.wikimedia.org/wikipedia/commons/c/ca/1x1.png"),
                callback = { _ ->
                    it()
                }
            )
        }
        Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = true
            override fun onNewData(segments: List<Segment>) {
                // just be here
            }
        })
        SegmentsCacheImpl(context, ExponeaGson.instance).set(SegmentTest.getSegmentsData())
        val exponeaPrefs = ExponeaPreferencesImpl(context).apply {
            setDouble(SessionManagerImpl.PREF_SESSION_START, 2.0)
            setDouble(SessionManagerImpl.PREF_SESSION_END, 3.0)
        }
        PushTokenRepositoryProvider.get(context).setTrackedToken("token", 10L, TokenType.FCM, true)
        CampaignRepositoryImpl(ExponeaGson.instance, exponeaPrefs).set(
            CampaignData(Uri.parse(CAMPAIGN_UNIVERSAL_LINK_URL))
        )
        val cookieRepo = UniqueIdentifierRepositoryImpl(exponeaPrefs)
        cookieRepo.get()
        CustomerIdsRepositoryImpl(ExponeaGson.instance, cookieRepo, exponeaPrefs).set(CustomerIds(hashMapOf(
            "registered" to "StopIntegrationCustomerId"
        )))
        DeviceInitiatedRepositoryImpl(exponeaPrefs).set(true)
        ExponeaConfigRepository.set(context, ExponeaConfiguration(projectToken = "mock-token"))
        InAppContentBlockDisplayStateRepositoryImpl(exponeaPrefs).apply {
            setDisplayed(InAppContentBlockManagerImplTest.buildMessage(), Date())
            setInteracted(InAppContentBlockManagerImplTest.buildMessage(), Date())
        }
        HtmlNormalizedCacheImpl(context, exponeaPrefs).apply {
            this.set("12345", "<html></html>", HtmlNormalizer.NormalizedResult())
        }
        waitForIt {
            FontCacheImpl(context).preload(
                urls = listOf("http://themes.googleusercontent.com/static/fonts/abeezee/v1/JYPhMn-3Xw-JGuyB-fEdNA.ttf"),
                callback = { _ ->
                    it()
                }
            )
        }
        InAppMessagesCacheImpl(context, ExponeaGson.instance).set(arrayListOf(
            InAppMessageTest.buildInAppMessageWithRichstyle("id1"),
            InAppMessageTest.buildInAppMessageWithoutRichstyle("id3")
        ))
        InAppMessageDisplayStateRepositoryImpl(exponeaPrefs, ExponeaGson.instance).apply {
            setDisplayed(InAppMessageTest.buildInAppMessageWithRichstyle("id1"), Date())
            setInteracted(InAppMessageTest.buildInAppMessageWithRichstyle("id1"), Date())
        }
        val telemetryManager = TelemetryManager(context.applicationContext as Application)
        telemetryManager.start()
        telemetryManager.reportLog(this, "test")
    }

    private fun validateEmptySdkData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(0, EventRepositoryImpl(context).count())
        val appInboxCache = AppInboxCacheImpl(context, ExponeaGson.instance)
        assertEquals(0, appInboxCache.getMessages().size)
        assertNull(appInboxCache.getSyncToken())
        assertFalse(
            DrawableCacheImpl(context).has("https://upload.wikimedia.org/wikipedia/commons/c/ca/1x1.png")
        )
        assertNull(SegmentsCacheImpl(context, ExponeaGson.instance).get())
        assertEquals(0, Exponea.segmentationDataCallbacks.size)
        val exponeaPrefs = ExponeaPreferencesImpl(context)
        assertEquals(-1.0, exponeaPrefs.getDouble(SessionManagerImpl.PREF_SESSION_START, -1.0))
        assertEquals(-1.0, exponeaPrefs.getDouble(SessionManagerImpl.PREF_SESSION_END, -1.0))
        val pushRepo = PushTokenRepositoryProvider.get(context)
        assertNull(pushRepo.getLastTrackDateInMilliseconds())
        assertNull(pushRepo.get())
        val campaignRepo = CampaignRepositoryImpl(ExponeaGson.instance, exponeaPrefs)
        assertNull(campaignRepo.get())
        assertFalse(DeviceInitiatedRepositoryImpl(exponeaPrefs).get())
        assertNull(ExponeaConfigRepository.get(context))
        val cbDisplayStateRepo = InAppContentBlockDisplayStateRepositoryImpl(exponeaPrefs)
        val cbDisplayState = cbDisplayStateRepo.get(InAppContentBlockManagerImplTest.buildMessage())
        assertNull(cbDisplayState.displayedLast)
        assertNull(cbDisplayState.interactedLast)
        val htmlCache = HtmlNormalizedCacheImpl(context, exponeaPrefs)
        assertNull(htmlCache.get("12345", "<html></html>"))
        val fontCache = FontCacheImpl(context)
        assertFalse(
            fontCache.has("http://themes.googleusercontent.com/static/fonts/abeezee/v1/JYPhMn-3Xw-JGuyB-fEdNA.ttf")
        )
        val inAppCache = InAppMessagesCacheImpl(context, ExponeaGson.instance)
        assertEquals(0, inAppCache.get().size)
        val iaDisplayStateRepo = InAppMessageDisplayStateRepositoryImpl(exponeaPrefs, ExponeaGson.instance)
        val iaDisplayState = iaDisplayStateRepo.get(InAppMessageTest.buildInAppMessageWithRichstyle("id1"))
        assertNull(iaDisplayState.displayed)
        assertNull(iaDisplayState.interacted)
        // UniqueIdentifierRepositoryImpl:
        assertEquals("", exponeaPrefs.getString(UniqueIdentifierRepositoryImpl.key, ""))
        // CustomerIdsRepositoryImpl:
        assertEquals("", exponeaPrefs.getString(CustomerIdsRepositoryImpl.PREFS_CUSTOMERIDS, ""))
        val telemetryManagerPrefs = TelemetryManager.getSharedPreferences(context.applicationContext as Application)
        assertEquals("", telemetryManagerPrefs.getString(INSTALL_ID_KEY, ""))
        assertEquals(0, Exponea.telemetry?.crashManager?.latestLogMessages?.size ?: 0)
        assertNotEquals(Exponea.telemetry?.crashManager, Thread.getDefaultUncaughtExceptionHandler())
    }

    private fun validateNonEmptySdkData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotEquals(0, EventRepositoryImpl(context).count())
        val appInboxCache = AppInboxCacheImpl(context, ExponeaGson.instance)
        assertNotEquals(0, appInboxCache.getMessages().size)
        assertNotNull(appInboxCache.getSyncToken())
        assertTrue(
            DrawableCacheImpl(context).has("https://upload.wikimedia.org/wikipedia/commons/c/ca/1x1.png")
        )
        assertNotNull(SegmentsCacheImpl(context, ExponeaGson.instance).get())
        assertNotEquals(0, Exponea.segmentationDataCallbacks.size)
        val exponeaPrefs = ExponeaPreferencesImpl(context)
        assertNotEquals(-1.0, exponeaPrefs.getDouble(SessionManagerImpl.PREF_SESSION_START, -1.0))
        assertNotEquals(-1.0, exponeaPrefs.getDouble(SessionManagerImpl.PREF_SESSION_END, -1.0))
        val pushRepo = PushTokenRepositoryProvider.get(context)
        assertNotNull(pushRepo.getLastTrackDateInMilliseconds())
        assertNotNull(pushRepo.get())
        val campaignRepo = CampaignRepositoryImpl(ExponeaGson.instance, exponeaPrefs)
        assertNotNull(campaignRepo.get())
        assertTrue(DeviceInitiatedRepositoryImpl(exponeaPrefs).get())
        assertNotNull(ExponeaConfigRepository.get(context))
        val cbDisplayStateRepo = InAppContentBlockDisplayStateRepositoryImpl(exponeaPrefs)
        val cbDisplayState = cbDisplayStateRepo.get(InAppContentBlockManagerImplTest.buildMessage())
        assertNotNull(cbDisplayState.displayedLast)
        assertNotNull(cbDisplayState.interactedLast)
        val htmlCache = HtmlNormalizedCacheImpl(context, exponeaPrefs)
        assertNotNull(htmlCache.get("12345", "<html></html>"))
        val fontCache = FontCacheImpl(context)
        assertTrue(
            fontCache.has("http://themes.googleusercontent.com/static/fonts/abeezee/v1/JYPhMn-3Xw-JGuyB-fEdNA.ttf")
        )
        val inAppCache = InAppMessagesCacheImpl(context, ExponeaGson.instance)
        assertNotEquals(0, inAppCache.get().size)
        val iaDisplayStateRepo = InAppMessageDisplayStateRepositoryImpl(exponeaPrefs, ExponeaGson.instance)
        val iaDisplayState = iaDisplayStateRepo.get(InAppMessageTest.buildInAppMessageWithRichstyle("id1"))
        assertNotNull(iaDisplayState.displayed)
        assertNotNull(iaDisplayState.interacted)
        // UniqueIdentifierRepositoryImpl:
        assertNotEquals("", exponeaPrefs.getString(UniqueIdentifierRepositoryImpl.key, ""))
        // CustomerIdsRepositoryImpl:
        assertNotEquals("", exponeaPrefs.getString(CustomerIdsRepositoryImpl.PREFS_CUSTOMERIDS, ""))
        val telemetryManagerPrefs = TelemetryManager.getSharedPreferences(context.applicationContext as Application)
        assertNotEquals("", telemetryManagerPrefs.getString(INSTALL_ID_KEY, ""))
        assertNotEquals(0, Exponea.telemetry?.crashManager?.latestLogMessages?.size ?: 0)
        assertEquals(Exponea.telemetry?.crashManager, Thread.getDefaultUncaughtExceptionHandler())
    }
}

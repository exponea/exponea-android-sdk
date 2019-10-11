package com.exponea.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.util.currentTimeSeconds
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AnonymizeTest : ExponeaSDKTest() {

    @Before
    fun init() {
        skipInstallEvent()
        val context = ApplicationProvider.getApplicationContext<Context>()
        Exponea.init(context, FlushManagerTest.configuration)
        waitUntilFlushed()
        Exponea.flushMode = FlushMode.MANUAL
    }

    @Test
    fun testAnonymize() {
        val testFirebaseToken = "TEST_FIREBASE_TOKEN"
        val previousId = Exponea.component.customerIdsRepository.get().cookie

        Exponea.trackEvent(
                eventType = "test",
                properties = PropertiesList(hashMapOf("name" to "test")),
                timestamp = currentTimeSeconds()
        )
        Exponea.trackPushToken(testFirebaseToken)

        val previousFirebaseToken = Exponea.component.firebaseTokenRepository.get()

        Exponea.anonymize()

        val newFirebaseToken = Exponea.component.firebaseTokenRepository.get()
        assertEquals(previousFirebaseToken, newFirebaseToken)

        val newId = Exponea.component.customerIdsRepository.get().cookie
        assertNotEquals(previousId, newId)

        val list = Exponea.component.eventRepository.all()
        assertEquals(list.size, 2)

        val typeList = list.map {
            it.item.type
        }
        assertTrue(typeList.contains(Constants.EventTypes.sessionStart))
    }
}
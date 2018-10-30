package com.exponea.sdk

import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.util.currentTimeSeconds
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AnonymizeTest {

    @Before
    fun init() {
        val context = RuntimeEnvironment.application

        Exponea.init(context, FlushManagerTest.configuration)
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
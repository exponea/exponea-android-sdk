package com.exponea.sdk

import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.PropertiesList
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.Date
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
        val previousId = Exponea.component.customerIdsRepository.get().cookie
        Exponea.trackEvent(
            eventType = "test",
            properties = PropertiesList(hashMapOf("name" to "test")),
            timestamp = Date().time
        )

        Exponea.anonymize()
        val newId = Exponea.component.customerIdsRepository.get().cookie
        assertNotEquals(previousId, newId)
        val list = Exponea.component.eventRepository.all()
        assertEquals(list.size, 1)
        val typeList = list.map {
            it.item.type
        }
        assertTrue(typeList.contains(Constants.EventTypes.sessionStart))
    }
}
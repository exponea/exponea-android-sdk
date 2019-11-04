package com.exponea.sdk.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.util.currentTimeSeconds
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultPropertiesTest : ExponeaSDKTest() {

    companion object {
        val CONFIG = ExponeaConfiguration()
        val PROPS_1 = PropertiesList(hashMapOf("my_custom_property" to "CustomPropValue"))
        val PROPS_2 = PropertiesList(hashMapOf("first_name" to "NewName"))
        val DEFAULT_PROPS = hashMapOf("default_name" to "DefaultValue")

        @BeforeClass
        @JvmStatic
        fun setup() {
            CONFIG.projectToken = "TestTokem"
            CONFIG.authorization = "TestTokenAuthentication"
            CONFIG.defaultProperties.putAll(DEFAULT_PROPS)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
        }
    }

    private lateinit var repo: EventRepository

    @Before
    fun prepareForTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        skipInstallEvent()
        Exponea.init(context, CONFIG)
        waitUntilFlushed()
        Exponea.flushMode = FlushMode.MANUAL

        repo = Exponea.component.eventRepository

        // Clean event repository for testing purposes
        repo.clear()
        val a = repo.all()
        assertEquals(a.size, 0)
    }

    @Test
    fun `Test default properties added`() {
        Exponea.trackEvent(
                eventType = "test_default_properties_1",
                timestamp = currentTimeSeconds(),
                properties = PROPS_1
        )
        val events = repo.all()
        assertEquals(1, events.size)

        var event = events.first()
        assertEquals("test_default_properties_1", event.item.type)
        assertEquals(2, event.item.properties?.size)

        var itemProperties = event.item.properties!!

        assertTrue(itemProperties.containsKey("my_custom_property"))
        assertEquals("CustomPropValue", itemProperties["my_custom_property"])
        assertTrue(itemProperties.containsKey("default_name"))
        assertEquals("DefaultValue", itemProperties["default_name"])
    }

    @Test
    fun `Test default properties not accumulating`() {
        Exponea.trackEvent(
                eventType = "test_default_properties_1",
                timestamp = currentTimeSeconds(),
                properties = PROPS_1
        )

        Exponea.trackEvent(
                eventType = "test_default_properties_2",
                timestamp = currentTimeSeconds(),
                properties = PROPS_2
        )

        val events = repo.all()
        assertEquals(2, events.size)

        events.sortBy { it.item.type }
        val event = events.last()
        assertEquals("test_default_properties_2", event.item.type)
        assertEquals(2, event.item.properties?.size)

        val itemProperties = event.item.properties!!

        assertTrue(itemProperties.containsKey("first_name"))
        assertEquals("NewName", itemProperties["first_name"])
        assertTrue(itemProperties.containsKey("default_name"))
        assertEquals("DefaultValue", itemProperties["default_name"])
    }
}

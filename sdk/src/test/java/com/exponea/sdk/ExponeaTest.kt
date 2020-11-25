package com.exponea.sdk

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.testutil.ExponeaSDKTest
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExponeaTest : ExponeaSDKTest() {
    @Test
    fun `should get null as customer cookie before initialized`() {
        assertNull(Exponea.customerCookie)
    }

    @Test
    fun `should get customer cookie after initialized`() {
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(ApplicationProvider.getApplicationContext(), ExponeaConfiguration())
        assertNotNull(Exponea.customerCookie)
    }

    @Test
    fun `should get current customer cookie after anonymize`() {
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(ApplicationProvider.getApplicationContext(), ExponeaConfiguration())
        val cookie1 = Exponea.customerCookie
        assertNotNull(cookie1)
        Exponea.anonymize()
        val cookie2 = Exponea.customerCookie
        assertNotNull(cookie2)
        assertNotEquals(cookie1, cookie2)
    }
}

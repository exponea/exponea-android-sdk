package com.exponea.sdk.models

import com.exponea.sdk.testutil.ExponeaSDKTest
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CustomerIdsTest : ExponeaSDKTest() {

    @Test
    fun testConstructor() {
        val mapWithoutCookie = hashMapOf<String, String?>(
                "registered" to "email",
                "phone" to "132132131"
        )

        val mapWithCookie = hashMapOf<String, String?>(
                "registered" to "email",
                "phone" to "132132131",
                "cookie" to "Cookie"
        )

        var customerIds = CustomerIds(mapWithCookie)
        assertTrue(customerIds.toHashMap()["cookie"] == null)

        assertTrue(customerIds.toHashMap()["registered"] == "email")

        customerIds = CustomerIds(mapWithoutCookie)

        assertTrue(customerIds.toHashMap()["cookie"] == null)
        customerIds.cookie = "cookiee"

        assertTrue(customerIds.toHashMap()["cookie"] == "cookiee")
        assertTrue(customerIds.toHashMap()["registered"] == "email")
    }

    fun testCookieGetter() {
        val customerIds = CustomerIds()
        assertTrue(customerIds.cookie == null)
        customerIds.cookie = "cookiee"
        assertTrue(customerIds.cookie == "cookie")
    }
}

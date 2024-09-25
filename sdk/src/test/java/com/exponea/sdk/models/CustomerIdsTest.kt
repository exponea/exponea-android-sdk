package com.exponea.sdk.models

import com.exponea.sdk.testutil.ExponeaSDKTest
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
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

    @Test
    fun testCookieGetter() {
        val customerIds = CustomerIds()
        assertTrue(customerIds.cookie == null)
        customerIds.cookie = "cookie"
        assertTrue(customerIds.cookie == "cookie")
    }

    @Test
    fun `should compare CustomerIds correctly`() {
        val equalVariants = listOf(
            Pair(CustomerIds(), CustomerIds()),
            Pair(CustomerIds(hashMapOf()), CustomerIds(hashMapOf())),
            Pair(
                CustomerIds(hashMapOf()).apply { cookie = "cookie" },
                CustomerIds(hashMapOf()).apply { cookie = "cookie" }),
            Pair(CustomerIds("cookie"), CustomerIds("cookie")),
            Pair(CustomerIds(hashMapOf("registered" to "user1")), CustomerIds(hashMapOf("registered" to "user1"))),
            Pair(
                CustomerIds(hashMapOf("registered" to "user1", "email" to "test@user.com")),
                CustomerIds(hashMapOf("registered" to "user1", "email" to "test@user.com"))
            ),
            Pair(
                CustomerIds(hashMapOf("email" to "test@user.com", "registered" to "user1")),
                CustomerIds(hashMapOf("registered" to "user1", "email" to "test@user.com"))
            ),
            Pair(
                CustomerIds(hashMapOf("registered" to "user1")).apply { cookie = "cookie" },
                CustomerIds(hashMapOf("registered" to "user1")).apply { cookie = "cookie" }
            ),
            Pair(
                CustomerIds(hashMapOf("registered" to "user1", "email" to "test@user.com")).apply { cookie = "cookie" },
                CustomerIds(hashMapOf("registered" to "user1", "email" to "test@user.com")).apply { cookie = "cookie" }
            ),
            Pair(
                CustomerIds(hashMapOf("email" to "test@user.com", "registered" to "user1")).apply { cookie = "cookie" },
                CustomerIds(hashMapOf("registered" to "user1", "email" to "test@user.com")).apply { cookie = "cookie" }
            )
        )
        equalVariants.forEach {
            assertEquals(it.first, it.second)
        }
        val notEqualVariants = listOf(
            Pair(
                CustomerIds(hashMapOf()).apply { cookie = "cookie1" },
                CustomerIds(hashMapOf()).apply { cookie = "cookie2" }),
            Pair(CustomerIds("cookie1"), CustomerIds("cookie2")),
            Pair(CustomerIds(hashMapOf("registered" to "user1")), CustomerIds(hashMapOf("registered" to "user2"))),
            Pair(
                CustomerIds(hashMapOf("registered" to "user1", "email" to "test@user.com")),
                CustomerIds(hashMapOf("registered" to "user2", "email" to "test@user.com"))
            ),
            Pair(
                CustomerIds(hashMapOf("email" to "test@user.com", "registered" to "user1")),
                CustomerIds(hashMapOf("registered" to "user2", "email" to "test@user.com"))
            ),
            Pair(
                CustomerIds(hashMapOf("registered" to "user1")).apply { cookie = "cookie" },
                CustomerIds(hashMapOf("registered" to "user2")).apply { cookie = "cookie" }
            ),
            Pair(
                CustomerIds(hashMapOf("registered" to "user1", "email" to "test@user.com")).apply { cookie = "cookie" },
                CustomerIds(hashMapOf("registered" to "user2", "email" to "test@user.com")).apply { cookie = "cookie" }
            ),
            Pair(
                CustomerIds(hashMapOf("email" to "test@user.com", "registered" to "user1")).apply { cookie = "cookie" },
                CustomerIds(hashMapOf("registered" to "user2", "email" to "test@user.com")).apply { cookie = "cookie" }
            )
        )
        notEqualVariants.forEach {
            assertNotEquals(it.first, it.second)
        }
    }
}

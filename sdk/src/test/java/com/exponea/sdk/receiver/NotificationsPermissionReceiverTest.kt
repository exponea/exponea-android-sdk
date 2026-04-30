package com.exponea.sdk.receiver

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class NotificationsPermissionReceiverTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun setNotificationsEnabled(enabled: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowOf(notificationManager).setNotificationsEnabled(enabled)
    }

    // Common: areNotificationsEnabled() is the first gate on all API levels
    @Test
    fun `should return false when notifications are disabled`() {
        setNotificationsEnabled(false)
        assertFalse(NotificationsPermissionReceiver.isPermissionGranted(context))
    }

    // Pre-Android 13 (API < 33): POST_NOTIFICATIONS runtime permission does not exist,
    // so only the system notification setting (areNotificationsEnabled) determines the result.
    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `should return true when notifications enabled on pre-TIRAMISU`() {
        setNotificationsEnabled(true)
        assertTrue(NotificationsPermissionReceiver.isPermissionGranted(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `should return false when notifications disabled on pre-TIRAMISU`() {
        setNotificationsEnabled(false)
        assertFalse(NotificationsPermissionReceiver.isPermissionGranted(context))
    }

    // Android 13+ (API 33 / TIRAMISU): both areNotificationsEnabled AND the POST_NOTIFICATIONS
    // runtime permission must be satisfied. Either one being denied results in false.
    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `should return true when notifications enabled and runtime permission granted on TIRAMISU`() {
        setNotificationsEnabled(true)
        shadowOf(context as android.app.Application).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(NotificationsPermissionReceiver.isPermissionGranted(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `should return false when notifications enabled but runtime permission denied on TIRAMISU`() {
        setNotificationsEnabled(true)
        shadowOf(context as android.app.Application).denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(NotificationsPermissionReceiver.isPermissionGranted(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `should return false when notifications disabled regardless of runtime permission on TIRAMISU`() {
        setNotificationsEnabled(false)
        shadowOf(context as android.app.Application).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(NotificationsPermissionReceiver.isPermissionGranted(context))
    }
}

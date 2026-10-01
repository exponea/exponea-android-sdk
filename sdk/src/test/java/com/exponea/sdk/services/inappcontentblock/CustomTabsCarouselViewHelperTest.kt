package com.exponea.sdk.services.inappcontentblock

import android.content.ComponentName
import android.content.Context
import android.os.Looper
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import com.exponea.sdk.models.LoggerCallback
import com.exponea.sdk.util.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
internal class CustomTabsCarouselViewHelperTest {
    private val context = mockk<Context>(relaxed = true)
    private val client = mockk<CustomTabsClient>()
    private val session = mockk<CustomTabsSession>()
    private val callback = mockk<CustomTabsCallback>()
    private val component = ComponentName("test", "TabsService")
    private val connections = mutableListOf<CustomTabsServiceConnection>()
    private val bound = CountDownLatch(1)

    @Before
    fun before() {
        mockkStatic(CustomTabsClient::class)
        every { CustomTabsClient.getPackageName(any(), null) } returns "test"
        every { CustomTabsClient.bindCustomTabsService(any(), any(), any()) } answers {
            synchronized(connections) { connections.add(arg(2)) }
            bound.countDown()
            true
        }
        every { client.warmup(0) } returns true
        every { client.newSession(callback) } returns session
    }

    @After
    fun after() {
        unmockkStatic(CustomTabsClient::class)
    }

    private fun connection(index: Int = 0): CustomTabsServiceConnection {
        assertTrue(bound.await(5, TimeUnit.SECONDS))
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (synchronized(connections) { connections.size } <= index && System.nanoTime() < deadline) Thread.sleep(10)
        return synchronized(connections) { connections[index] }
    }

    private fun awaitSession(helper: CustomTabsCarouselViewHelper) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (helper.getSession() == null && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertSame(session, helper.getSession())
    }

    @Test
    fun `slow warmup does not block main thread or publish session early`() {
        val enteredWarmup = CountDownLatch(1)
        val releaseWarmup = CountDownLatch(1)
        every { client.warmup(0) } answers {
            assertTrue(Thread.currentThread() !== Looper.getMainLooper().thread)
            enteredWarmup.countDown()
            assertTrue(releaseWarmup.await(5, TimeUnit.SECONDS))
            true
        }
        val helper = CustomTabsCarouselViewHelper(context, callback)
        try {
            helper.bindCustomTabsService()
            connection().onCustomTabsServiceConnected(component, client)
            assertTrue(enteredWarmup.await(5, TimeUnit.SECONDS))
            assertEquals(Looper.getMainLooper(), Looper.myLooper())
            assertNull(helper.getSession())
            releaseWarmup.countDown()
            awaitSession(helper)
        } finally {
            releaseWarmup.countDown()
            helper.unbindCustomTabsService()
        }
    }

    @Test
    fun `detach during warmup discards result and reattach creates a new session`() {
        val enteredWarmup = CountDownLatch(1)
        val releaseWarmup = CountDownLatch(1)
        every { client.warmup(0) } answers {
            enteredWarmup.countDown()
            releaseWarmup.await(5, TimeUnit.SECONDS)
            true
        }
        val helper = CustomTabsCarouselViewHelper(context, callback)
        helper.bindCustomTabsService()
        val oldConnection = connection()
        oldConnection.onCustomTabsServiceConnected(component, client)
        assertTrue(enteredWarmup.await(5, TimeUnit.SECONDS))
        helper.unbindCustomTabsService()
        assertNull(helper.getSession())
        releaseWarmup.countDown()
        helper.bindCustomTabsService()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (synchronized(connections) { connections.size } < 2 && System.nanoTime() < deadline) Thread.sleep(10)
        val newConnection = synchronized(connections) { connections[1] }
        oldConnection.onCustomTabsServiceConnected(component, client)
        assertNull(helper.getSession())
        newConnection.onCustomTabsServiceConnected(component, client)
        awaitSession(helper)
        newConnection.onServiceDisconnected(component)
        assertNull(helper.getSession())
        helper.unbindCustomTabsService()
    }

    @Test
    fun `false bind unbinds attempted connection and permits explicit retry`() {
        every { CustomTabsClient.bindCustomTabsService(any(), any(), any()) } answers {
            synchronized(connections) { connections.add(arg(2)) }
            bound.countDown()
            synchronized(connections) { connections.size > 1 }
        }
        val helper = CustomTabsCarouselViewHelper(context, callback)
        helper.bindCustomTabsService()
        val first = connection()
        verify(timeout = 5000) { context.unbindService(first) }
        assertNull(helper.getSession())
        helper.bindCustomTabsService()
        connection(1).onCustomTabsServiceConnected(component, client)
        awaitSession(helper)
        helper.unbindCustomTabsService()
    }

    @Test
    fun `thrown bind unbinds attempted connection and permits explicit retry`() {
        every { CustomTabsClient.bindCustomTabsService(any(), any(), any()) } answers {
            synchronized(connections) { connections.add(arg(2)) }
            bound.countDown()
            if (synchronized(connections) { connections.size } == 1) throw IllegalStateException("bind failed")
            true
        }
        val helper = CustomTabsCarouselViewHelper(context, callback)
        helper.bindCustomTabsService()
        verify(timeout = 5000) { context.unbindService(connection()) }
        helper.bindCustomTabsService()
        connection(1).onCustomTabsServiceConnected(component, client)
        awaitSession(helper)
        helper.unbindCustomTabsService()
    }

    @Test
    fun `package lookup exception permits explicit retry`() {
        var lookups = 0
        val failedLookup = CountDownLatch(1)
        every { CustomTabsClient.getPackageName(any(), null) } answers {
            if (++lookups == 1) {
                failedLookup.countDown()
                throw IllegalStateException("lookup failed")
            }
            "test"
        }
        val helper = CustomTabsCarouselViewHelper(context, callback)
        helper.bindCustomTabsService()
        assertTrue(failedLookup.await(5, TimeUnit.SECONDS))
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (synchronized(connections) { connections.isEmpty() } && System.nanoTime() < deadline) {
            helper.bindCustomTabsService()
            Thread.sleep(10)
        }
        connection().onCustomTabsServiceConnected(component, client)
        awaitSession(helper)
        helper.unbindCustomTabsService()
    }

    @Test
    fun `stale failed bind does not clear a newer session`() {
        val firstBindStarted = CountDownLatch(1)
        val releaseFirstBind = CountDownLatch(1)
        every { CustomTabsClient.bindCustomTabsService(any(), any(), any()) } answers {
            synchronized(connections) { connections.add(arg(2)) }
            bound.countDown()
            if (synchronized(connections) { connections.size } == 1) {
                firstBindStarted.countDown()
                releaseFirstBind.await(5, TimeUnit.SECONDS)
                false
            } else true
        }
        val helper = CustomTabsCarouselViewHelper(context, callback)
        try {
            helper.bindCustomTabsService()
            assertTrue(firstBindStarted.await(5, TimeUnit.SECONDS))
            helper.unbindCustomTabsService()
            helper.bindCustomTabsService()
            connection(1).onCustomTabsServiceConnected(component, client)
            awaitSession(helper)
            releaseFirstBind.countDown()
            verify(timeout = 5000) { context.unbindService(connection(0)) }
            assertSame(session, helper.getSession())
        } finally {
            releaseFirstBind.countDown()
            helper.unbindCustomTabsService()
        }
    }

    @Test
    fun `false warmup result still creates a session`() {
        every { client.warmup(0) } returns false
        val helper = CustomTabsCarouselViewHelper(context, callback)
        helper.bindCustomTabsService()
        connection().onCustomTabsServiceConnected(component, client)
        awaitSession(helper)
        helper.unbindCustomTabsService()
    }

    @Test
    fun `warmup exception still creates a session`() {
        val attemptedWarmup = CountDownLatch(1)
        every { client.warmup(0) } answers {
            attemptedWarmup.countDown()
            throw IllegalStateException("provider failed")
        }
        val helper = CustomTabsCarouselViewHelper(context, callback)
        helper.bindCustomTabsService()
        connection().onCustomTabsServiceConnected(component, client)
        assertTrue(attemptedWarmup.await(5, TimeUnit.SECONDS))
        awaitSession(helper)
        verify(exactly = 1) { client.newSession(callback) }
        helper.unbindCustomTabsService()
    }

    @Test
    fun `detach during throwing warmup discards session`() {
        val enteredWarmup = CountDownLatch(1)
        val releaseWarmup = CountDownLatch(1)
        every { client.warmup(0) } answers {
            enteredWarmup.countDown()
            releaseWarmup.await(5, TimeUnit.SECONDS)
            throw IllegalStateException("warmup failed")
        }
        val helper = CustomTabsCarouselViewHelper(context, callback)
        try {
            helper.bindCustomTabsService()
            connection().onCustomTabsServiceConnected(component, client)
            assertTrue(enteredWarmup.await(5, TimeUnit.SECONDS))
            helper.unbindCustomTabsService()
            releaseWarmup.countDown()
            shadowOf(Looper.getMainLooper()).idle()
            assertNull(helper.getSession())
            verify(exactly = 0) { client.newSession(any()) }
        } finally {
            releaseWarmup.countDown()
            helper.unbindCustomTabsService()
        }
    }

    @Test
    fun `unbind exception is logged and does not escape background job`() {
        val failure = IllegalStateException("cleanup failed")
        val logged = CountDownLatch(1)
        val observer = object : LoggerCallback {
            override fun onLog(level: Logger.Level, message: String, throwable: Throwable?) {
                if (message.contains("service unbind failed") && throwable === failure) logged.countDown()
            }
        }
        Logger.registerCallback(observer)
        try {
            every { context.unbindService(any()) } throws failure
            val helper = CustomTabsCarouselViewHelper(context, callback)
            helper.bindCustomTabsService()
            connection()
            helper.unbindCustomTabsService()
            assertTrue(logged.await(5, TimeUnit.SECONDS))
        } finally {
            Logger.unregisterCallback(observer)
        }
    }

    @Test
    fun `unregistered connection is tolerated`() {
        val failure = IllegalArgumentException("not registered")
        val logged = CountDownLatch(1)
        val observer = object : LoggerCallback {
            override fun onLog(level: Logger.Level, message: String, throwable: Throwable?) {
                if (message.contains("was not registered")) logged.countDown()
            }
        }
        Logger.registerCallback(observer)
        try {
            every { context.unbindService(any()) } throws failure
            val helper = CustomTabsCarouselViewHelper(context, callback)
            helper.bindCustomTabsService()
            connection()
            helper.unbindCustomTabsService()
            assertTrue(logged.await(5, TimeUnit.SECONDS))
        } finally {
            Logger.unregisterCallback(observer)
        }
    }
}

package com.exponea.sdk.services

import com.exponea.sdk.Exponea
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExponeaDeintegrateManagerTests {

    @After
    fun resetSdkState() {
        Exponea.isStopped = false
    }

    @Test
    fun `Should register all deintegration callbacks`() {
        val manager = ExponeaDeintegrateManager()
        var invokeCount = 0
        manager.registerForIntegrationStopped(object : OnIntegrationStoppedCallback {
            override fun onIntegrationStopped() {
                invokeCount += 1
            }
        })
        manager.registerForIntegrationStopped(object : OnIntegrationStoppedCallback {
            override fun onIntegrationStopped() {
                invokeCount += 1
            }
        })
        assertEquals(0, invokeCount)
        assertEquals(2, manager.onIntegrationStoppedCallbacks.size)
    }

    @Test
    fun `Should ignore deintegration callbacks for already stopped SDK`() {
        Exponea.isStopped = true
        val manager = ExponeaDeintegrateManager()
        var invokeCount = 0
        manager.registerForIntegrationStopped(object : OnIntegrationStoppedCallback {
            override fun onIntegrationStopped() {
                invokeCount += 1
            }
        })
        manager.registerForIntegrationStopped(object : OnIntegrationStoppedCallback {
            override fun onIntegrationStopped() {
                invokeCount += 1
            }
        })
        assertEquals(0, invokeCount)
        assertEquals(0, manager.onIntegrationStoppedCallbacks.size)
    }

    @Test
    fun `Should invoke all deintegration callbacks by polling`() {
        val manager = ExponeaDeintegrateManager()
        var invokeCount = 0
        manager.registerForIntegrationStopped(object : OnIntegrationStoppedCallback {
            override fun onIntegrationStopped() {
                invokeCount += 1
            }
        })
        manager.registerForIntegrationStopped(object : OnIntegrationStoppedCallback {
            override fun onIntegrationStopped() {
                invokeCount += 1
            }
        })
        manager.notifyDeintegration()
        assertEquals(2, invokeCount)
        assertEquals(0, manager.onIntegrationStoppedCallbacks.size)
    }

    @Test
    fun `Should invoke all deintegration callbacks by polling safely`() {
        val manager = ExponeaDeintegrateManager()
        var invokeCount = 0
        manager.registerForIntegrationStopped(object : OnIntegrationStoppedCallback {
            override fun onIntegrationStopped() {
                invokeCount += 1
            }
        })
        manager.registerForIntegrationStopped(object : OnIntegrationStoppedCallback {
            override fun onIntegrationStopped() {
                throw RuntimeException("Expected error")
            }
        })
        manager.registerForIntegrationStopped(object : OnIntegrationStoppedCallback {
            override fun onIntegrationStopped() {
                invokeCount += 1
            }
        })
        manager.notifyDeintegration()
        assertEquals(2, invokeCount)
        assertEquals(0, manager.onIntegrationStoppedCallbacks.size)
    }

    @Test
    fun `Should not invoke removed deintegration callbacks`() {
        val manager = ExponeaDeintegrateManager()
        var invokeCount = 0
        val callbackToRemove = object : OnIntegrationStoppedCallback {
            override fun onIntegrationStopped() {
                invokeCount += 1
            }
        }
        manager.registerForIntegrationStopped(callbackToRemove)
        manager.registerForIntegrationStopped(object : OnIntegrationStoppedCallback {
            override fun onIntegrationStopped() {
                invokeCount += 1
            }
        })
        manager.unregisterForIntegrationStopped(callbackToRemove)
        manager.notifyDeintegration()
        assertEquals(1, invokeCount)
        assertEquals(0, manager.onIntegrationStoppedCallbacks.size)
    }
}

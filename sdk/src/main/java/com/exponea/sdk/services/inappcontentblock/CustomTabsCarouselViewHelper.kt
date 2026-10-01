package com.exponea.sdk.services.inappcontentblock

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import com.exponea.sdk.util.Logger
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class CustomTabsCarouselViewHelper(
    context: Context,
    private val tabsCallback: CustomTabsCallback
) {
    private val contextRef = WeakReference(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error ->
        Logger.e(this, "InAppCbCarousel: Custom Tabs background job failed", error)
    })
    private val lock = Any()
    private var generation = 0L
    private var serviceGeneration = 0L
    private var active = false
    private var bindingJob: Job? = null
    private var preparationJob: Job? = null
    private var connection: CustomTabsServiceConnection? = null
    @Volatile private var tabsSession: CustomTabsSession? = null

    fun bindCustomTabsService() {
        val currentGeneration: Long
        synchronized(lock) {
            if (active) return
            active = true
            currentGeneration = ++generation
            bindingJob = ioScope.launch { bind(currentGeneration) }
        }
    }

    private fun bind(currentGeneration: Long) {
        val context = contextRef.get()
        if (context == null) {
            releaseFailedBind(currentGeneration)
            return
        }
        try {
            val packageName = CustomTabsClient.getPackageName(context, null)
            if (packageName.isNullOrBlank()) {
                Logger.w(this, "InAppCbCarousel: App that supports Custom Tabs has not been found")
                // Older Android versions may still bind without a package name.
            }
            synchronized(lock) {
                if (!active || generation != currentGeneration) return
            }
            val newConnection = object : CustomTabsServiceConnection() {
                override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                    val currentServiceGeneration: Long
                    synchronized(lock) {
                        if (!active || generation != currentGeneration) return
                        preparationJob?.cancel()
                        tabsSession = null
                        currentServiceGeneration = ++serviceGeneration
                        preparationJob = ioScope.launch {
                            prepareSession(client, currentGeneration, currentServiceGeneration)
                        }
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    synchronized(lock) {
                        if (generation != currentGeneration) return
                        ++serviceGeneration
                        preparationJob?.cancel()
                        preparationJob = null
                        tabsSession = null
                    }
                }
            }
            val bound = try {
                CustomTabsClient.bindCustomTabsService(context, packageName, newConnection)
            } catch (e: Exception) {
                Logger.e(this, "InAppCbCarousel: Custom Tabs service bind failed", e)
                releaseFailedBind(currentGeneration)
                unbind(context, newConnection)
                return
            }
            if (!bound) {
                Logger.w(this, "InAppCbCarousel: Custom Tabs service bind failed")
                releaseFailedBind(currentGeneration)
                unbind(context, newConnection)
                return
            }
            val stale = synchronized(lock) {
                if (active && generation == currentGeneration) {
                    connection = newConnection
                    false
                } else true
            }
            // Detach may happen while bindCustomTabsService is in progress.
            if (stale) unbind(context, newConnection)
        } catch (e: Exception) {
            Logger.e(this, "InAppCbCarousel: Custom Tabs setup failed", e)
            releaseFailedBind(currentGeneration)
        }
    }

    private fun releaseFailedBind(currentGeneration: Long) {
        synchronized(lock) {
            if (!active || generation != currentGeneration) return
            active = false
            bindingJob = null
            ++serviceGeneration
            preparationJob?.cancel()
            preparationJob = null
            tabsSession = null
            connection = null
        }
    }

    private fun prepareSession(client: CustomTabsClient, currentGeneration: Long, currentServiceGeneration: Long) {
        try {
            client.warmup(0)
        } catch (e: Exception) {
            Logger.e(this, "InAppCbCarousel: Custom Tabs warmup failed", e)
        }
        synchronized(lock) {
            if (!active || generation != currentGeneration || serviceGeneration != currentServiceGeneration) return
        }
        try {
            val session = client.newSession(tabsCallback)
            mainHandler.post {
                synchronized(lock) {
                    if (active && generation == currentGeneration && serviceGeneration == currentServiceGeneration) {
                        tabsSession = session
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e(this, "InAppCbCarousel: Custom Tabs session setup failed", e)
        }
    }

    fun unbindCustomTabsService() {
        val oldConnection: CustomTabsServiceConnection?
        synchronized(lock) {
            active = false
            ++generation
            ++serviceGeneration
            bindingJob?.cancel()
            bindingJob = null
            preparationJob?.cancel()
            preparationJob = null
            tabsSession = null
            oldConnection = connection
            connection = null
        }
        val context = contextRef.get()
        if (context != null && oldConnection != null) {
            ioScope.launch { unbind(context, oldConnection) }
        }
    }

    private fun unbind(context: Context, connection: CustomTabsServiceConnection) {
        try {
            context.unbindService(connection)
        } catch (e: IllegalArgumentException) {
            Logger.w(this, "InAppCbCarousel: Custom Tabs service was not registered, unbind skipped: ${e.message}")
        } catch (e: Exception) {
            Logger.e(this, "InAppCbCarousel: Custom Tabs service unbind failed", e)
        }
    }

    fun getSession(): CustomTabsSession? = tabsSession
}

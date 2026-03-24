package com.exponea.sdk.services.inappcontentblock

import android.content.ComponentName
import android.content.Context
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import com.exponea.sdk.util.Logger
import java.lang.ref.WeakReference

internal class CustomTabsCarouselViewHelper(
    context: Context,
    val tabsCallback: CustomTabsCallback
) {
    private val contextRef = WeakReference(context)
    private var tabsClient: CustomTabsClient? = null
    private var tabsSession: CustomTabsSession? = null
    private var tabsServiceConnection: CustomTabsServiceConnection? = null
    private var isCustomTabsServiceBound = false

    fun bindCustomTabsService() {
        val context = contextRef.get() ?: return

        val customTabsPackage = CustomTabsClient.getPackageName(context, null)
        if (customTabsPackage.isNullOrBlank()) {
            Logger.w(this, "InAppCbCarousel: App that supports Custom Tabs has not been found")
            // Still, try invoke `bindCustomTabsService` for old Android
        }

        val connection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(
                name: ComponentName,
                client: CustomTabsClient
            ) {
                tabsClient = client
                client.warmup(0)
                tabsSession = client.newSession(tabsCallback)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                tabsClient = null
                tabsSession = null
            }
        }

        val bound = CustomTabsClient.bindCustomTabsService(context, customTabsPackage, connection)
        if (bound) {
            tabsServiceConnection = connection
            isCustomTabsServiceBound = true
        } else {
            Logger.w(this, "InAppCbCarousel: Custom Tabs service bind failed")
        }
    }

    fun unbindCustomTabsService() {
        if (!isCustomTabsServiceBound) {
            tabsClient = null
            tabsSession = null
            tabsServiceConnection = null
            return
        }

        val context = contextRef.get()
        val connection = tabsServiceConnection
        if (context != null && connection != null) {
            try {
                context.unbindService(connection)
            } catch (e: IllegalArgumentException) {
                Logger.w(this, "InAppCbCarousel: Custom Tabs service was not registered, unbind skipped: ${e.message}")
            }
        } else {
            Logger.w(
                this,
                "InAppCbCarousel: Skipped Custom Tabs unbind " +
                        "(contextLost=${context == null}, connectionMissing=${connection == null})")
        }

        isCustomTabsServiceBound = false
        tabsClient = null
        tabsSession = null
        tabsServiceConnection = null
    }

    fun getSession(): CustomTabsSession? {
        return tabsSession
    }
}

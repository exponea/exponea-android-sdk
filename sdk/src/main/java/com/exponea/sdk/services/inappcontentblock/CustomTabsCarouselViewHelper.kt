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
        }.also { tabsServiceConnection = it }

        CustomTabsClient.bindCustomTabsService(context, customTabsPackage, connection)
    }

    fun unbindCustomTabsService() {
        tabsServiceConnection?.let {
            contextRef.get()?.unbindService(it)
        }

        tabsClient = null
        tabsSession = null
        tabsServiceConnection = null
    }

    fun getSession(): CustomTabsSession? {
        return tabsSession
    }
}

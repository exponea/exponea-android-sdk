package com.exponea.sdk.services.inappcontentblock

import android.os.Bundle
import androidx.browser.customtabs.CustomTabsCallback
import com.exponea.sdk.util.Logger
import java.lang.ref.WeakReference

internal class CustomTabsCarouselViewCallback(
    viewController: ContentBlockCarouselViewController
) : CustomTabsCallback() {
    private val viewControllerRef = WeakReference(viewController)

    override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
        when (navigationEvent) {
            NAVIGATION_FAILED, TAB_HIDDEN -> {
                Logger.i(this, "InAppCbCarousel: User returns to app")
                viewControllerRef.get()?.onViewBecomeForeground()
            }
            else -> {
                Logger.v(this, "InAppCbCarousel: Web Navigation event: $navigationEvent")
            }
        }
    }
}

package com.exponea.sdk.shadows

import android.content.res.Resources
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowResources

@Suppress("UNUSED_PARAMETER")
@Implements(Resources::class)
class ShadowResourcesWithAllResources : ShadowResources() {
    @Implementation
    fun getIdentifier(name: String, defType: String, defPackage: String): Int {
        return 123
    }

    @Implementation
    fun getResourceName(resid: Int): String {
        return "mock"
    }
}

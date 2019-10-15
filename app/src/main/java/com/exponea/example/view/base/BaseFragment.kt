package com.exponea.example.view.base

import androidx.fragment.app.Fragment
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.PropertiesList

open class BaseFragment : androidx.fragment.app.Fragment() {

    /**
     * Tracks certain screen that customer have visited
     * @param pageName - Name of the screen
     */
    fun trackPage(pageName: String) {
        val properties = PropertiesList(hashMapOf("name" to pageName))

        Exponea.trackEvent(
            eventType = "page_view",
            properties = properties
        )
    }
}

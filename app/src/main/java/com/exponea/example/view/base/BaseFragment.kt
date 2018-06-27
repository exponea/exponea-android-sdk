package com.exponea.example.view.base

import android.support.v4.app.Fragment
import com.exponea.example.App
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.PropertiesList

open class BaseFragment : Fragment() {

    /**
     * Tracks certain screen that customer have visited
     * @param pageName - Name of the screen
     */
    fun trackPage(pageName: String) {
        val properties = PropertiesList(hashMapOf(Pair("name", pageName)))

        Exponea.trackCustomerEvent(
                eventType =  "page_view",
                properties = properties,
                timestamp = null
        )
    }
}
package com.exponea.example.view.base

import android.support.v4.app.Fragment
import com.exponea.example.App
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.Route
import java.util.*

open class BaseFragment : Fragment() {

    fun trackPage(pageName: String) {
        val userID = App.instance.userIdManager.uniqueUserID

        Exponea.trackEvent(
                eventType =  "page_view",
                customerId = CustomerIds(cookie = userID),
                properties = hashMapOf( Pair("name", pageName)),
                route = Route.TRACK_EVENTS
        )
    }
}
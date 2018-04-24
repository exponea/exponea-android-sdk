package com.exponea.example.view.base

import android.support.v4.app.Fragment
import com.exponea.example.App
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import java.util.*

open class BaseFragment : Fragment() {

    fun trackPage(pageName: String) {
        val timestamp = (Date().time / 1000).toDouble()
        val userID = App.instance.userIdManager.uniqueUserID

        Exponea.trackEvent(
                "PageView",
                timestamp,
                CustomerIds(cookie = userID),
                hashMapOf(
                        Pair("Name", pageName)
                )
        )
    }
}
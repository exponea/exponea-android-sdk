package com.exponea.sdk.services

import android.content.Context
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.exponea.sdk.models.MessageItem

interface AppInboxProvider {
    fun getAppInboxButton(context: Context): Button
    fun getAppInboxListView(context: Context, onItemClicked: (MessageItem, Int) -> Unit): View
    fun getAppInboxListFragment(context: Context): Fragment
    fun getAppInboxDetailFragment(context: Context, messageId: String): Fragment
    fun getAppInboxDetailView(context: Context, messageId: String): View
}

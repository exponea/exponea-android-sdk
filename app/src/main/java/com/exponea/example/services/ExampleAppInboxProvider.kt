package com.exponea.example.services

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.transition.Fade
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.services.DefaultAppInboxProvider
import com.exponea.sdk.view.AppInboxDetailFragment
import com.exponea.sdk.view.AppInboxDetailView
import com.exponea.sdk.view.AppInboxListFragment
import com.exponea.sdk.view.AppInboxListView

class ExampleAppInboxProvider : DefaultAppInboxProvider() {
    override fun getAppInboxButton(context: Context): Button {
        val appInboxButton = super.getAppInboxButton(context)
        appInboxButton.setTextColor(Color.BLACK)
        return appInboxButton
    }
    override fun getAppInboxListView(context: Context, onItemClicked: (MessageItem, Int) -> Unit): View {
        val appInboxListView = super.getAppInboxListView(context, onItemClicked)
        val typedView = appInboxListView as AppInboxListView
        typedView.statusErrorTitleView.setTextColor(Color.RED)
        return typedView
    }
    override fun getAppInboxListFragment(context: Context): Fragment {
        val appInboxListFragment = super.getAppInboxListFragment(context)
        val typedFragment = appInboxListFragment as AppInboxListFragment
        typedFragment.enterTransition = Fade()
        typedFragment.returnTransition = Fade()
        return typedFragment
    }
    override fun getAppInboxDetailFragment(context: Context, messageId: String): Fragment {
        val appInboxDetailFragment = super.getAppInboxDetailFragment(context, messageId)
        val typedFragment = appInboxDetailFragment as AppInboxDetailFragment
        typedFragment.enterTransition = Fade()
        typedFragment.returnTransition = Fade()
        return appInboxDetailFragment
    }
    override fun getAppInboxDetailView(context: Context, messageId: String): View {
        val appInboxDetailView = super.getAppInboxDetailView(context, messageId)
        val typedView = appInboxDetailView as AppInboxDetailView
        typedView.titleView.textSize = 32f
        return appInboxDetailView
    }
}

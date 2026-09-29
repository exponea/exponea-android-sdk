package com.exponea.example.services

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
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
        val density = context.resources.displayMetrics.density
        return super.getAppInboxButton(context).apply {
            text = "Inbox"
            isAllCaps = false
            setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            setTextColor(Color.parseColor("#111111"))
            setTypeface(typeface, Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFD500"))
                cornerRadius = 8f * density
            }
            minHeight = (44 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun getAppInboxListView(context: Context, onItemClicked: (MessageItem, Int) -> Unit): View {
        val view = super.getAppInboxListView(context, onItemClicked) as AppInboxListView
        view.statusErrorTitleView.setTextColor(Color.RED)
        return view
    }

    override fun getAppInboxListFragment(context: Context): Fragment {
        return (super.getAppInboxListFragment(context) as AppInboxListFragment).apply {
            enterTransition = Fade()
            returnTransition = Fade()
        }
    }

    override fun getAppInboxDetailFragment(context: Context, messageId: String): Fragment {
        return (super.getAppInboxDetailFragment(context, messageId) as AppInboxDetailFragment).apply {
            enterTransition = Fade()
            returnTransition = Fade()
        }
    }

    override fun getAppInboxDetailView(context: Context, messageId: String): View {
        return (super.getAppInboxDetailView(context, messageId) as AppInboxDetailView).apply {
            titleView.textSize = 32f
        }
    }
}

package com.exponea.sdk.style.appinbox

import android.content.Context
import android.view.View
import android.view.ViewGroup.OnHierarchyChangeListener
import android.widget.Button
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener
import com.exponea.sdk.databinding.MessageInboxListItemBinding
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.services.DefaultAppInboxProvider
import com.exponea.sdk.util.MessageItemViewHolder
import com.exponea.sdk.view.AppInboxDetailView
import com.exponea.sdk.view.AppInboxListView
import com.exponea.style.AppInboxStyle

class StyledAppInboxProvider(private val appInboxStyle: AppInboxStyle) : DefaultAppInboxProvider() {

    override fun getAppInboxButton(context: Context): Button {
        val button = super.getAppInboxButton(context)
        // Default Button style is set to capitalize text
        button.isAllCaps = false
        appInboxStyle.appInboxButton?.applyTo(button)
        return button
    }

    override fun getAppInboxDetailView(context: Context, messageId: String): View {
        val view = super.getAppInboxDetailView(context, messageId) as AppInboxDetailView
        appInboxStyle.detailView?.let { detailViewStyle ->
            detailViewStyle.applyTo(view)
            detailViewStyle.button?.let { button ->
                // actions may be populated later
                // (performance) register listener only if button style is set
                view.actionsContainerView.setOnHierarchyChangeListener(object : OnHierarchyChangeListener {
                    override fun onChildViewAdded(parent: View?, child: View?) {
                        if (parent != view.actionsContainerView || child == null || child !is Button) {
                            return
                        }
                        if (child.layoutParams is LinearLayout.LayoutParams) {
                            val layoutParams = child.layoutParams as LinearLayout.LayoutParams
                            layoutParams.topMargin = 8
                            child.layoutParams = layoutParams
                        }
                        button.applyTo(child)
                    }

                    override fun onChildViewRemoved(p0: View?, p1: View?) {
                        // no action
                    }
                })
            }
        }
        return view
    }

    override fun getAppInboxListView(context: Context, onItemClicked: (MessageItem, Int) -> Unit): View {
        val view = super.getAppInboxListView(context, onItemClicked) as AppInboxListView
        appInboxStyle.listView?.applyTo(view)
        appInboxStyle.listView?.list?.item?.let { itemStyle ->
            // items are populated and shown later
            // (performance) register listener only if item style is set
            view.listView.addOnChildAttachStateChangeListener(object : OnChildAttachStateChangeListener {
                override fun onChildViewAttachedToWindow(view: View) {
                    itemStyle.applyTo(MessageItemViewHolder(MessageInboxListItemBinding.bind(view)))
                }

                override fun onChildViewDetachedFromWindow(view: View) {
                    // nothing to do
                }
            })
        }
        return view
    }
}

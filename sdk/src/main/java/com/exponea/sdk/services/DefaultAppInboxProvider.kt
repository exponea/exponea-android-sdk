package com.exponea.sdk.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.exponea.sdk.Exponea
import com.exponea.sdk.R
import com.exponea.sdk.manager.AppInboxAdapter
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.models.MessageItemAction
import com.exponea.sdk.models.MessageItemAction.Type
import com.exponea.sdk.models.MessageItemAction.Type.APP
import com.exponea.sdk.models.MessageItemAction.Type.BROWSER
import com.exponea.sdk.models.MessageItemAction.Type.DEEPLINK
import com.exponea.sdk.models.MessageItemAction.Type.NO_ACTION
import com.exponea.sdk.models.MessageItemContent
import com.exponea.sdk.util.Logger
import com.exponea.sdk.view.AppInboxDetailFragment
import com.exponea.sdk.view.AppInboxDetailView
import com.exponea.sdk.view.AppInboxListActivity
import com.exponea.sdk.view.AppInboxListFragment
import com.exponea.sdk.view.AppInboxListView
import kotlin.random.Random

open class DefaultAppInboxProvider : AppInboxProvider {

    private val SUPPORTED_MESSAGE_ACTION_TYPES: List<Type> = listOf(
        BROWSER, DEEPLINK
    )

    override fun getAppInboxButton(context: Context): Button {
        var button = LayoutInflater.from(context).inflate(R.layout.message_inbox_button, null, false) as Button
        button.setOnClickListener {
            context.startActivity(Intent(context, AppInboxListActivity::class.java))
        }
        return button
    }

    override fun getAppInboxListView(context: Context, onItemClicked: (MessageItem, Int) -> Unit): View {
        val listView = AppInboxListView(context)
        listView.listView.layoutManager = LinearLayoutManager(context)
        val onItemClickedProxy = { message: MessageItem, position: Int ->
            message.read = true
            listView.listView.adapter?.notifyItemChanged(position)
            Exponea.markAppInboxAsRead(message.id, null)
            onItemClicked.invoke(message, position)
        }
        val bitmapCache = Exponea.getComponent()?.inAppMessagesBitmapCache
        if (bitmapCache == null) {
            throw Exception("Exponea SDK was not initialized properly!")
        }
        val appInboxAdapter = AppInboxAdapter(bitmapCache = bitmapCache, onItemClicked = onItemClickedProxy)
        listView.listView.adapter = appInboxAdapter
        showLoading(listView)
        Exponea.fetchAppInbox { data ->
            if (data == null) {
                Logger.e(this, "[AppInbox] Error while loading AppInbox")
                showError(listView)
                return@fetchAppInbox
            }
            if (data.isEmpty()) {
                Logger.i(this, "[AppInbox] AppInbox loaded but is empty")
                showEmptyMessageInbox(listView)
                return@fetchAppInbox
            }
            Logger.i(this, "[AppInbox] AppInbox loaded")
            showMessageInbox(listView)
            appInboxAdapter.replaceData(data)
        }
        return listView
    }

    override fun getAppInboxListFragment(context: Context): Fragment {
        return AppInboxListFragment()
    }

    override fun getAppInboxDetailFragment(context: Context, messageId: String): Fragment {
        return AppInboxDetailFragment.buildInstance(messageId)
    }

    override fun getAppInboxDetailView(context: Context, messageId: String): View {
        val detailView = AppInboxDetailView(context)
        Exponea.fetchAppInboxItem(messageId, { message ->
            message?.let {
                showMessageDetail(it, detailView)
            }
        })
        return detailView
    }

    private fun showLoading(target: AppInboxListView) {
        target.statusContainterView.visibility = View.VISIBLE
        target.statusProgressView.visibility = View.VISIBLE
        target.statusEmptyTitleView.visibility = View.GONE
        target.statusEmptyMessageView.visibility = View.GONE
        target.statusErrorTitleView.visibility = View.GONE
        target.statusErrorMessageView.visibility = View.GONE
        target.listView.visibility = View.GONE
    }

    private fun showMessageInbox(target: AppInboxListView) {
        target.statusContainterView.visibility = View.GONE
        target.statusProgressView.visibility = View.GONE
        target.statusEmptyTitleView.visibility = View.GONE
        target.statusEmptyMessageView.visibility = View.GONE
        target.statusErrorTitleView.visibility = View.GONE
        target.statusErrorMessageView.visibility = View.GONE
        target.listView.visibility = View.VISIBLE
    }

    private fun showEmptyMessageInbox(target: AppInboxListView) {
        target.statusContainterView.visibility = View.VISIBLE
        target.statusProgressView.visibility = View.GONE
        target.statusEmptyTitleView.visibility = View.VISIBLE
        target.statusEmptyMessageView.visibility = View.VISIBLE
        target.statusErrorTitleView.visibility = View.GONE
        target.statusErrorMessageView.visibility = View.GONE
        target.listView.visibility = View.GONE
    }

    private fun showError(target: AppInboxListView) {
        target.statusContainterView.visibility = View.VISIBLE
        target.statusProgressView.visibility = View.GONE
        target.statusEmptyTitleView.visibility = View.GONE
        target.statusEmptyMessageView.visibility = View.GONE
        target.statusErrorTitleView.visibility = View.VISIBLE
        target.statusErrorMessageView.visibility = View.VISIBLE
        target.listView.visibility = View.GONE
    }

    private fun showMessageDetail(source: MessageItem, target: AppInboxDetailView) {
        val dataSource = source.content
        val bitmapCache = Exponea.getComponent()?.inAppMessagesBitmapCache
        if (bitmapCache == null) {
            throw Exception("Exponea SDK was not initialized properly!")
        }
        dataSource?.imageUrl?.let { imageUrl ->
            bitmapCache.preload(listOf(imageUrl), { loaded ->
                if (loaded) {
                    val bitmapToShow = bitmapCache.get(imageUrl)
                    Handler(Looper.getMainLooper()).post {
                        target.imageView.setImageBitmap(bitmapToShow)
                    }
                }
            })
        }
        val receivedMillis: Long = dataSource?.createdAt?.times(1000)?.toLong() ?: System.currentTimeMillis()
        target.receivedTimeView.text = DateUtils.getRelativeTimeSpanString(
            receivedMillis,
            System.currentTimeMillis(),
            DateUtils.DAY_IN_MILLIS
        )
        target.titleView.text = dataSource?.title ?: ""
        target.contentView.text = dataSource?.message ?: ""
        val actions = readActions(dataSource, target.context)
        if (actions.isEmpty()) {
            target.actionsContainerView.visibility = View.GONE
        } else {
            target.actionsContainerView.visibility = View.VISIBLE
            target.actionsContainerView.removeAllViews()
            actions.forEach { messageAction ->
                val actionButton = LayoutInflater
                    .from(target.context)
                    .inflate(R.layout.message_inbox_action_button, null, false) as Button
                actionButton.text = messageAction.title
                actionButton.setOnClickListener {
                    invokeAction(messageAction, source, target.context)
                }
                target.actionsContainerView.addView(actionButton)
            }
        }
    }

    private fun readActions(source: MessageItemContent?, context: Context): List<MessageItemAction> {
        var target = mutableListOf<MessageItemAction>()
        source?.action?.let { mainAction ->
            if (SUPPORTED_MESSAGE_ACTION_TYPES.contains(mainAction.type)) {
                target.add(MessageItemAction().apply {
                    type = mainAction.type
                    url = mainAction.url
                    title = context.getString(R.string.exponea_inbox_mainActionTitle)
                })
            }
        }
        source?.actions
            ?.filter { SUPPORTED_MESSAGE_ACTION_TYPES.contains(it.type) }
            ?.forEach { target.add(it) }
        return target
    }

    private fun invokeAction(action: MessageItemAction, message: MessageItem, context: Context) {
        Logger.i(this, "Invoking AppInbox action \"${action.title}\"")
        Exponea.trackAppInboxClick(action, message)
        when (action.type) {
            APP -> {
                // nothing to do, app is already shown
            }
            BROWSER -> {
                openAction(action, context)
            }
            DEEPLINK -> {
                openAction(action, context)
            }
            NO_ACTION -> {
                // nothing to do, no action provided
            }
        }
    }

    private fun openAction(action: MessageItemAction, context: Context) {
        if (useOlderApi()) {
            val intentWithUrl = Intent(Intent.ACTION_VIEW)
            intentWithUrl.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            action.url?.let { url -> if (url.isNotBlank()) intentWithUrl.data = Uri.parse(url) }
            context.startActivity(intentWithUrl)
            return
        }
        // Newer Api
        val urlIntent = Intent(Intent.ACTION_VIEW)
        urlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        action.url?.let { url -> if (url.isNotBlank()) urlIntent.data = Uri.parse(url) }
        PendingIntent.getActivities(
            context.applicationContext,
            Random.nextInt(),
            arrayOf(urlIntent),
            MessagingUtils.getPendingIntentFlags()
        ).send()
    }

    private fun useOlderApi(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.M
}

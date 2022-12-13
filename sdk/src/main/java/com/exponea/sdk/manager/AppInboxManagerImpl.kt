package com.exponea.sdk.manager

import android.os.Handler
import android.os.Looper
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.models.Result
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.repository.AppInboxCache
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.InAppMessageBitmapCache
import com.exponea.sdk.util.Logger
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.Call
import okhttp3.Response

internal class AppInboxManagerImpl(
    private val fetchManager: FetchManager,
    private val bitmapCache: InAppMessageBitmapCache,
    private val configuration: ExponeaConfiguration,
    private val customerIdsRepository: CustomerIdsRepository,
    private val api: ExponeaService,
    private val appInboxCache: AppInboxCache
) : AppInboxManager {

    private val SUPPORTED_MESSAGE_TYPES: List<String> = listOf("push")

    public override fun markMessageAsRead(messageId: String, callback: ((Boolean) -> Unit)?) {
        appInboxCache.getMessages().forEach { msg ->
            if (msg.id == messageId) {
                msg.read = true
            }
        }
        // ensure to message change is stored
        appInboxCache.setMessages(appInboxCache.getMessages())
        api.postReadFlagAppInbox(
            exponeaProject = configuration.mainExponeaProject,
            customerIds = customerIdsRepository.get(),
            messageIds = listOf(messageId)
        ).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseCode = response.code
                Logger.d(this, "Response Code: $responseCode")
                if (response.isSuccessful) {
                    Logger.i(this, "AppInbox marked as read")
                    Handler(Looper.getMainLooper()).post {
                        callback?.invoke(true)
                    }
                } else {
                    Logger.e(this, "AppInbox marking as read failed. ${response.message}")
                    Handler(Looper.getMainLooper()).post {
                        callback?.invoke(false)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Logger.e(this, "AppInbox marking as read failed: $e")
                Handler(Looper.getMainLooper()).post {
                    callback?.invoke(true)
                }
            }
        })
    }

    public override fun fetchAppInbox(callback: ((List<MessageItem>?) -> Unit)) {
        fetchManager.fetchAppInbox(
            exponeaProject = configuration.mainExponeaProject,
            customerIds = customerIdsRepository.get(),
            syncToken = appInboxCache.getSyncToken(),
            onSuccess = { result ->
                Logger.i(this, "AppInbox loaded successfully")
                onAppInboxDataLoaded(result, callback)
            },
            onFailure = {
                Logger.e(this, "AppInbox loading failed. ${it.results.message}")
                Handler(Looper.getMainLooper()).post {
                    callback.invoke(null)
                }
            }
        )
    }

    private fun onAppInboxDataLoaded(result: Result<ArrayList<MessageItem>?>, callback: (List<MessageItem>) -> Unit) {
        result.syncToken?.let {
            appInboxCache.setSyncToken(it)
        }
        val messages = result.results ?: arrayListOf()
        var supportedMessages = messages.filter { SUPPORTED_MESSAGE_TYPES.contains(it.type) }
        val imageUrls = supportedMessages
            .mapNotNull { messageItem -> messageItem.content?.imageUrl }
            .filter { imageUrl -> !imageUrl.isNullOrBlank() }
        appInboxCache.addMessages(supportedMessages)
        var allMessages = appInboxCache.getMessages()
        if (imageUrls.isEmpty()) {
            Handler(Looper.getMainLooper()).post {
                callback.invoke(allMessages)
            }
            return
        }
        val counter = AtomicInteger(imageUrls.size)
        imageUrls.forEach { imageUrl ->
            bitmapCache.preload(listOf(imageUrl), {
                if (counter.decrementAndGet() <= 0) {
                    Handler(Looper.getMainLooper()).post {
                        callback.invoke(allMessages)
                    }
                }
            })
        }
    }

    override fun fetchAppInboxItem(messageId: String, callback: (MessageItem?) -> Unit) {
        fetchAppInbox { messages ->
            val foundMessage = messages?.find { messageItem -> messageItem.id == messageId }
            callback.invoke(foundMessage)
        }
    }

    override fun clear() {
        appInboxCache.clear()
    }

    override fun onEventCreated(event: Event, type: EventType) {
        if (EventType.TRACK_CUSTOMER == type) {
            Logger.i(this, "CustomerIDs are updated, clearing AppInbox messages")
            clear()
        }
    }
}

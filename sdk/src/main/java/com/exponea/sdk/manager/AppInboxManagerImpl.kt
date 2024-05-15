package com.exponea.sdk.manager

import com.exponea.sdk.models.AppInboxMessateType.UNKNOWN
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.models.Result
import com.exponea.sdk.repository.AppInboxCache
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.services.ExponeaProjectFactory
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.runOnBackgroundThread
import com.exponea.sdk.util.runOnMainThread
import java.util.concurrent.atomic.AtomicInteger

internal class AppInboxManagerImpl(
    private val fetchManager: FetchManager,
    private val drawableCache: DrawableCache,
    private val customerIdsRepository: CustomerIdsRepository,
    private val appInboxCache: AppInboxCache,
    private val projectFactory: ExponeaProjectFactory
) : AppInboxManager {

    public override fun markMessageAsRead(message: MessageItem, callback: ((Boolean) -> Unit)?) {
        if (message.syncToken == null || message.customerIds.isEmpty()) {
            Logger.e(this, "Unable to mark message ${message.id} as read, try to fetch AppInbox")
            runOnMainThread {
                callback?.invoke(false)
            }
            return
        }
        message.read = true
        // ensure to message change is stored
        markCachedMessageAsRead(message.id)
        requireMutualExponeaProject { expoProject ->
            if (expoProject.authorization == null) {
                Logger.e(this, "AppInbox loading failed. Authorization token is missing")
                runOnMainThread {
                    callback?.invoke(false)
                }
            }
            val customerIds = CustomerIds(HashMap(message.customerIds)).apply {
                message.customerIds.get(CustomerIds.COOKIE)?.let {
                    cookie = it
                }
            }
            fetchManager.markAppInboxAsRead(
                exponeaProject = expoProject,
                customerIds = customerIds,
                syncToken = message.syncToken!!,
                messageIds = listOf(message.id),
                onSuccess = {
                    Logger.i(this, "AppInbox marked as read")
                    runOnMainThread {
                        callback?.invoke(true)
                    }
                },
                onFailure = {
                    Logger.e(this, "AppInbox marking as read failed. ${it.results.message}")
                    runOnMainThread {
                        callback?.invoke(false)
                    }
                }
            )
        }
    }

    private fun markCachedMessageAsRead(messageId: String) {
        val messages = appInboxCache.getMessages()
        messages.forEach { cachedMessage ->
            if (cachedMessage.id == messageId) {
                cachedMessage.read = true
            }
        }
        appInboxCache.setMessages(messages)
    }

    public override fun fetchAppInbox(callback: ((List<MessageItem>?) -> Unit)) {
        requireMutualExponeaProject { expoProject ->
            if (expoProject.authorization == null) {
                Logger.e(this, "AppInbox loading failed. Authorization token is missing")
                runOnMainThread {
                    callback.invoke(null)
                }
                return@requireMutualExponeaProject
            }
            val customerIds = customerIdsRepository.get()
            fetchManager.fetchAppInbox(
                exponeaProject = expoProject,
                customerIds = customerIds,
                syncToken = appInboxCache.getSyncToken(),
                onSuccess = { result ->
                    Logger.i(this, "AppInbox loaded successfully")
                    enhanceMessages(result.results, customerIds, result.syncToken)
                    onAppInboxDataLoaded(result, callback)
                },
                onFailure = {
                    Logger.e(this, "AppInbox loading failed. ${it.results.message}")
                    runOnMainThread {
                        callback.invoke(null)
                    }
                }
            )
        }
    }

    private fun requireMutualExponeaProject(onTokenCallback: (ExponeaProject) -> Unit) {
        runOnBackgroundThread {
            onTokenCallback.invoke(projectFactory.mutualExponeaProject)
        }
    }

    /**
     * Adds customerIds and syncToken into every MessageItem in list.
     * These values will be used for tracking and markAsRead future calls
     */
    private fun enhanceMessages(messages: List<MessageItem>?, customerIds: CustomerIds, syncToken: String?) {
        if (messages == null || messages.isEmpty()) {
            return
        }
        messages.forEach { messageItem ->
            messageItem.customerIds = customerIds.toHashMap()
            messageItem.syncToken = syncToken
        }
    }

    private fun onAppInboxDataLoaded(result: Result<ArrayList<MessageItem>?>, callback: (List<MessageItem>) -> Unit) {
        result.syncToken?.let {
            appInboxCache.setSyncToken(it)
        }
        val messages = result.results ?: arrayListOf()
        var supportedMessages = messages.filter { it.type != UNKNOWN }
        val imageUrls = supportedMessages
            .mapNotNull { messageItem -> messageItem.content?.imageUrl }
            .filter { imageUrl -> !imageUrl.isNullOrBlank() }
        appInboxCache.addMessages(supportedMessages)
        var allMessages = appInboxCache.getMessages()
        if (imageUrls.isEmpty()) {
            runOnMainThread {
                callback.invoke(allMessages)
            }
            return
        }
        val counter = AtomicInteger(imageUrls.size)
        imageUrls.forEach { imageUrl ->
            drawableCache.preload(listOf(imageUrl), {
                if (counter.decrementAndGet() <= 0) {
                    runOnMainThread {
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

    override fun reload() {
        appInboxCache.clear()
        fetchAppInbox { Logger.d(this, "AppInbox loaded") }
    }

    override fun onEventCreated(event: Event, type: EventType) {
        if (EventType.TRACK_CUSTOMER == type) {
            Logger.i(this, "CustomerIDs are updated, clearing AppInbox messages")
            reload()
        }
    }
}

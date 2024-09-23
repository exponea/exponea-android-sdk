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
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

internal class AppInboxManagerImpl(
    private val fetchManager: FetchManager,
    private val drawableCache: DrawableCache,
    private val customerIdsRepository: CustomerIdsRepository,
    private val appInboxCache: AppInboxCache,
    private val projectFactory: ExponeaProjectFactory
) : AppInboxManager {

    internal var lastCustomerIdsForFetch: CustomerIds? = null
    internal val isFetching = AtomicBoolean(false)
    internal val onFetchDoneCallbacks = LinkedBlockingQueue<(List<MessageItem>?) -> Unit>()

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
        val customerIds = customerIdsRepository.get()
        onFetchDoneCallbacks.add(callback)
        if (!isFetching.compareAndSet(false, true)) {
            Logger.d(this, "AppInbox fetch already in progress, waiting for response")
            lastCustomerIdsForFetch = customerIds
            return
        }
        invokeFetchAppInbox(customerIds)
    }

    private fun invokeFetchAppInbox(customerIds: CustomerIds) {
        requireMutualExponeaProject { expoProject ->
            if (expoProject.authorization == null) {
                Logger.e(this, "AppInbox loading failed. Authorization token is missing")
                notifyFetchCallbacks(null)
                isFetching.set(false)
                return@requireMutualExponeaProject
            }
            fetchManager.fetchAppInbox(
                exponeaProject = expoProject,
                customerIds = customerIds,
                syncToken = appInboxCache.getSyncToken(),
                onSuccess = { result ->
                    Logger.d(this, "AppInbox fetch is done for customerIds ${customerIds.toHashMap()}")
                    handleDataFetchResult(result, customerIds)
                },
                onFailure = {
                    Logger.e(this, "AppInbox loading failed. ${it.results.message}")
                    handleDataFetchResult(null, customerIds)
                }
            )
        }
    }

    private fun handleDataFetchResult(
        result: Result<ArrayList<MessageItem>?>?,
        processCustomerIds: CustomerIds
    ) {
        val fetchProcessIsValid: Boolean
        val lastCustomerIdsForFetchLocal = lastCustomerIdsForFetch
        if (lastCustomerIdsForFetchLocal == null) {
            fetchProcessIsValid = true
        } else if (lastCustomerIdsForFetchLocal.toHashMap() == processCustomerIds.toHashMap()) {
            fetchProcessIsValid = true
            lastCustomerIdsForFetch = null
        } else {
            fetchProcessIsValid = false
            Logger.w(
                this,
                "AppInbox fetch is outdated for last customer IDs ${lastCustomerIdsForFetchLocal.toHashMap()}"
            )
        }
        if (fetchProcessIsValid) {
            onAppInboxDataLoaded(result, processCustomerIds)
        } else {
            appInboxCache.clear()
            val customerIdsToRepeat = lastCustomerIdsForFetchLocal ?: customerIdsRepository.get()
            lastCustomerIdsForFetch = null
            Logger.i(this, "AppInbox fetch is going to repeat for ${customerIdsToRepeat.toHashMap()}")
            invokeFetchAppInbox(customerIdsToRepeat)
        }
    }

    private fun notifyFetchCallbacks(data: List<MessageItem>?) {
        val activeCallbacks = mutableListOf<(List<MessageItem>?) -> Unit>()
        onFetchDoneCallbacks.drainTo(activeCallbacks)
        activeCallbacks.forEach { activeCallback ->
            runOnMainThread {
                activeCallback.invoke(data)
            }
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
        messages?.forEach { messageItem ->
            messageItem.customerIds = customerIds.toHashMap()
            messageItem.syncToken = syncToken
        }
    }

    private fun onAppInboxDataLoaded(
        result: Result<ArrayList<MessageItem>?>?,
        customerIds: CustomerIds
    ) {
        if (customerIds.toHashMap() != customerIdsRepository.get().toHashMap()) {
            Logger.i(this, "AppInbox data for ${customerIds.toHashMap()} will replace old data")
            appInboxCache.clear()
        }
        val dataFetchFinalization = { appInboxData: List<MessageItem>? ->
            isFetching.set(false)
            notifyFetchCallbacks(appInboxData)
        }
        if (result == null) {
            // failure => no data
            Logger.e(this, "AppInbox loading failed")
            dataFetchFinalization(null)
        } else {
            // we have data => success
            Logger.i(this, "AppInbox loaded successfully")
            enhanceMessages(result.results, customerIds, result.syncToken)
            result.syncToken?.let {
                appInboxCache.setSyncToken(it)
            }
            val loadedMessages = result.results ?: arrayListOf()
            val supportedMessages = loadedMessages.filter { it.type != UNKNOWN }
            appInboxCache.addMessages(supportedMessages)
            val allMessages = appInboxCache.getMessages()
            val imageUrls = supportedMessages
                .mapNotNull { messageItem -> messageItem.content?.imageUrl }
                .filter { imageUrl -> imageUrl.isNotBlank() }
            drawableCache.preload(imageUrls) {
                dataFetchFinalization(allMessages)
            }
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
            fetchAppInbox { Logger.d(this, "AppInbox loaded") }
        }
    }
}

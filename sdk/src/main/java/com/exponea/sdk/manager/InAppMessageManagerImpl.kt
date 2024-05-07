package com.exponea.sdk.manager

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.TrackingConsentManager.MODE.CONSIDER_CONSENT
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.DeviceProperties
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.InAppMessageButton
import com.exponea.sdk.models.InAppMessageButtonType.BROWSER
import com.exponea.sdk.models.InAppMessageButtonType.DEEPLINK
import com.exponea.sdk.models.InAppMessagePayloadButton
import com.exponea.sdk.models.InAppMessageType
import com.exponea.sdk.repository.BitmapCache
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.InAppMessageDisplayStateRepository
import com.exponea.sdk.repository.InAppMessagesCache
import com.exponea.sdk.repository.SimpleFileCache
import com.exponea.sdk.services.ExponeaProjectFactory
import com.exponea.sdk.util.GdprTracking
import com.exponea.sdk.util.HtmlNormalizer
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.currentTimeSeconds
import com.exponea.sdk.util.ensureOnBackgroundThread
import com.exponea.sdk.util.logOnException
import com.exponea.sdk.util.runOnBackgroundThread
import com.exponea.sdk.util.runOnMainThread
import com.exponea.sdk.view.InAppMessagePresenter
import java.util.Date

internal data class InAppMessageShowRequest(
    val eventType: String,
    val properties: Map<String, Any?>,
    val eventTimestamp: Double,
    val requestedAt: Long,
    val customerIds: HashMap<String, String?>
)
internal class InAppMessageManagerImpl(
    private val customerIdsRepository: CustomerIdsRepository,
    private val inAppMessagesCache: InAppMessagesCache,
    private val fetchManager: FetchManager,
    private val displayStateRepository: InAppMessageDisplayStateRepository,
    private val bitmapCache: BitmapCache,
    private val fontCache: SimpleFileCache,
    private val presenter: InAppMessagePresenter,
    private val eventManager: TrackingConsentManager,
    private val projectFactory: ExponeaProjectFactory
) : InAppMessageManager {
    companion object {
        const val REFRESH_CACHE_AFTER = 1000 * 60 * 30 // when session is started and cache is older than this, refresh
        const val MAX_PENDING_MESSAGE_AGE = 1000 * 3 // time window to show pending message after preloading
        const val MAX_RETRY_COUNT = 3 // message fetch retry count for failure
    }

    private var isLoading = false
    private var pendingShowRequestsMap: Map<String, InAppMessageShowRequest> = mutableMapOf()
    internal var pendingShowRequests: List<InAppMessageShowRequest>
        get() {
            return pendingShowRequestsMap.values.toList()
        }
        set(value) {
            pendingShowRequestsMap = value
                .sortedBy { it.eventTimestamp }
                .associateBy { it.eventType }
        }

    private var sessionStartDate = Date()

    override fun reload(callback: ((Result<Unit>) -> Unit)?) {
        reload(
            customerIdsRepository.get().toHashMap(),
            callback,
            0
        )
    }

    internal fun reload(
        customerIds: HashMap<String, String?>,
        callback: ((Result<Unit>) -> Unit)? = null,
        retryCount: Int = 0
    ) {
        preloadStarted()
        val customerIdsForFetch = CustomerIds(HashMap(customerIds)).apply {
            // CustomerIds constructor removes cookie, add it back:
            cookie = customerIds[CustomerIds.COOKIE]
        }
        fetchManager.fetchInAppMessages(
            exponeaProject = projectFactory.mainExponeaProject,
            customerIds = customerIdsForFetch,
            onSuccess = { result ->
                if (areCustomerIdsActual(customerIds)) {
                    inAppMessagesCache.set(result.results)
                    bitmapCache.clearExcept(loadImageUrls(result.results))
                    Logger.d(this, "[InApp] In-app messages preloaded successfully")
                    preloadFinished()
                    callback?.invoke(Result.success(Unit))
                } else {
                    preloadFinished()
                    notifyAboutObsoleteFetch(callback)
                }
            },
            onFailure = {
                if (retryCount < MAX_RETRY_COUNT) {
                    Logger.w(this, "[InApp] Preloading in-app messages failed, going to retry")
                    reload(customerIds, callback, retryCount + 1)
                } else if (areCustomerIdsActual(customerIds)) {
                    Logger.e(this, "[InApp] Preloading in-app messages failed. ${it.results.message}")
                    preloadFinished()
                    callback?.invoke(Result.failure(Exception("Preloading in-app messages failed.")))
                } else {
                    preloadFinished()
                    notifyAboutObsoleteFetch(callback)
                }
            }
        )
    }

    private fun notifyAboutObsoleteFetch(callback: ((Result<Unit>) -> Unit)?) {
        Logger.w(this, "[InApp] In-app loading was done for obsolete customer IDs, ignoring result")
        preloadFinished()
        callback?.invoke(Result.failure(Exception("Preloading in-app messages expired.")))
    }

    private fun areCustomerIdsActual(customerIds: HashMap<String, String?>): Boolean {
        return customerIdsRepository.get().toHashMap() == customerIds
    }

    internal fun preloadStarted() {
        isLoading = true
    }

    internal fun preloadFinished() {
        isLoading = false
    }

    internal fun detectReloadMode(eventType: EventType, timestamp: Double): ReloadMode {
        if (eventType == EventType.PUSH_DELIVERED ||
            eventType == EventType.PUSH_OPENED ||
            eventType == EventType.SESSION_END
        ) {
            Logger.d(
                this,
                "[InApp] Auto-skipping messages fetch for type $eventType"
            )
            return ReloadMode.SHOW
        }
        if (eventType == EventType.TRACK_CUSTOMER) {
            Logger.d(this, "[InApp] Forcing messages fetch for type $eventType")
            return ReloadMode.FETCH_AND_SHOW
        }
        if (isLoading) {
            Logger.d(
                this,
                "[InApp] Skipping messages fetch for type $eventType because of running fetch"
            )
            return ReloadMode.STOP
        }
        val cacheExpired = (inAppMessagesCache.getTimestamp() + REFRESH_CACHE_AFTER) < timestamp
        if (cacheExpired) {
            Logger.d(
                this,
                "[InApp] Loading messages fetch for type $eventType because cache expires"
            )
            return ReloadMode.FETCH_AND_SHOW
        }
        Logger.d(
            this,
            "[InApp] Skipping messages fetch for type $eventType because cache is valid"
        )
        return ReloadMode.SHOW
    }

    override fun sessionStarted(sessionStartDate: Date) {
        Logger.d(this, "[InApp] Updating session start value to $sessionStartDate")
        this.sessionStartDate = sessionStartDate
    }

    private fun loadImageUrls(messages: List<InAppMessage>): List<String> {
        return messages
            .flatMap { loadImageUrls(it) }
            .distinct()
    }

    private fun loadImageUrls(message: InAppMessage): List<String> {
        val imageURLs = ArrayList<String>()
        if (message.messageType == InAppMessageType.FREEFORM) {
            imageURLs.addAll(HtmlNormalizer(bitmapCache, fontCache, message.payloadHtml!!).collectImages())
        } else if (!message.payload?.imageUrl.isNullOrEmpty()) {
            imageURLs.add(message.payload!!.imageUrl!!)
        }
        return imageURLs
    }

    internal fun pickPendingMessage(): Pair<InAppMessageShowRequest, InAppMessage>? {
        synchronized(this) {
            // collect messages by pending requests
            val currentCustomerIds = customerIdsRepository.get().toHashMap()
            val pendingMessages: List<Pair<InAppMessageShowRequest, InAppMessage>> = pendingShowRequests
                // if any In-app is shown, pick is meaningless
                .filter {
                    val isOtherMsgPresenting = !presenter.isPresenting()
                    if (!isOtherMsgPresenting) {
                        Logger.d(
                            this,
                            "[InApp] Show request ${it.eventType} skipped, another In-app message is shown"
                        )
                    }
                    return@filter isOtherMsgPresenting
                }
                // show request lifetime
                .filter {
                    val requestTimeStillValid = it.requestedAt + MAX_PENDING_MESSAGE_AGE > System.currentTimeMillis()
                    if (!requestTimeStillValid) {
                        Logger.d(
                            this,
                            "[InApp] Show request ${it.eventType} has time-outed"
                        )
                    }
                    return@filter requestTimeStillValid
                }
                // show request meant for current customer
                .filter {
                    val customerStillValid = it.customerIds == currentCustomerIds
                    if (!customerStillValid) {
                        Logger.d(
                            this,
                            "[InApp] Show request ${it.eventType} has been created for different customer"
                        )
                    }
                    return@filter customerStillValid
                }
                // message filter
                .mapNotNull { request ->
                    val topMessageForRequest = findMessagesByFilter(
                        request.eventType,
                        request.properties,
                        request.eventTimestamp
                    ).randomOrNull()
                    Logger.i(
                        this, "[InApp] Picking top message '${topMessageForRequest?.name ?: "none"}'" +
                        " for eventType ${request.eventType}"
                    )
                    return@mapNotNull topMessageForRequest?.let { Pair(request, it) }
                }
            Logger.d(this, "[InApp] Clearing all show requests because messages has been selected")
            clearPendingShowRequests()
            // find messages with highest priority
            Logger.i(
                this,
                "[InApp] Got ${pendingMessages.size} messages available to show." +
                    " ${pendingMessages.map { it.second.name }}"
            )
            val highestPriorityFound = pendingMessages.maxOfOrNull { it.second.priority ?: 0 } ?: 0
            val prioritizedMessages = pendingMessages.filter { (it.second.priority ?: 0) >= highestPriorityFound }
            // return random from priority messages or top priority message for single
            val topMessage = prioritizedMessages.randomOrNull()
            Logger.i(
                this,
                "[InApp] Picking top message '${topMessage?.second?.name ?: "none"}' to be shown."
            )
            return topMessage
        }
    }

    override fun findMessagesByFilter(
        eventType: String,
        properties: Map<String, Any?>,
        timestamp: Double?
    ): List<InAppMessage> {
        var messages = inAppMessagesCache.get()
        Logger.i(
            this,
            "[InApp] Picking in-app message for eventType $eventType. " +
                "${messages.size} messages available: ${messages.map { it.name } }."
        )
        messages = messages.filter {
                it.applyDateFilter(System.currentTimeMillis() / 1000) &&
                it.applyEventFilter(eventType, properties, timestamp) &&
                it.applyFrequencyFilter(displayStateRepository.get(it), sessionStartDate)
        }
        Logger.i(this,
            "[InApp] ${messages.size} messages available after filtering." +
                " Going to pick the highest priority messages."
        )
        val highestPriority = messages.mapNotNull { it.priority }.maxOrNull() ?: 0
        messages = messages.filter { (it.priority ?: 0) >= highestPriority }
        Logger.i(
            this,
            "[InApp] Got ${messages.size} messages with highest priority for eventType $eventType." +
                " ${messages.map { it.name } }"
        )
        return messages
    }

    internal fun show(message: InAppMessage) {
        if (message.variantId == -1 && !message.hasPayload()) {
            Logger.i(this, "[InApp] Only logging in-app message for control group '${message.name}'")
            runOnMainThread {
                Exponea.inAppMessageCallback.inAppMessageShow(message)
            }
            trackShowEvent(message)
            return
        }
        if (!message.hasPayload()) {
            Logger.e(this, "[InApp] Not showing message with empty payload '${message.name}'")
            return
        }
        Logger.i(this, "[InApp] Attempting to show in-app message '${message.name}'")
        val htmlPayload: HtmlNormalizer.NormalizedResult?
        if (message.messageType == InAppMessageType.FREEFORM && !message.payloadHtml.isNullOrEmpty()) {
            htmlPayload = HtmlNormalizer(bitmapCache, fontCache, message.payloadHtml).normalize()
        } else {
            htmlPayload = null
        }
        Logger.i(this, "[InApp] Posting show to main thread with delay ${message.delay ?: 0}ms.")
        runOnMainThread(message.delay ?: 0) {
            runCatching {
                val presented = presenter.show(
                    messageType = message.messageType,
                    payload = message.payload,
                    payloadHtml = htmlPayload,
                    timeout = message.timeout,
                    actionCallback = { activity, button ->
                        Logger.i(this, "In-app message button clicked!")
                        displayStateRepository.setInteracted(message, Date())
                        if (Exponea.inAppMessageCallback.trackActions) {
                            eventManager.trackInAppMessageClick(
                                message,
                                button.buttonText,
                                button.buttonLink,
                                CONSIDER_CONSENT
                            )
                        }
                        val buttonInfo = InAppMessageButton(button.buttonText, button.buttonLink)
                        runOnMainThread {
                            Exponea.inAppMessageCallback.inAppMessageAction(
                                message,
                                buttonInfo,
                                true,
                                activity
                            )
                        }
                        if (!Exponea.inAppMessageCallback.overrideDefaultBehavior) {
                            processInAppMessageAction(activity, button)
                        }
                    },
                    dismissedCallback = { activity, userInteraction ->
                        if (Exponea.inAppMessageCallback.trackActions) {
                            eventManager.trackInAppMessageClose(message, userInteraction, CONSIDER_CONSENT)
                        }
                        runOnMainThread {
                            Exponea.inAppMessageCallback.inAppMessageAction(
                                message,
                                null,
                                userInteraction,
                                activity
                            )
                        }
                    },
                    failedCallback = { error ->
                        if (Exponea.inAppMessageCallback.trackActions) {
                            trackError(message, error)
                        }
                    }
                )
                runOnBackgroundThread {
                    runCatching {
                        if (presented != null) {
                            runOnMainThread {
                                Exponea.inAppMessageCallback.inAppMessageShow(message)
                            }
                            trackShowEvent(message)
                        }
                    }.logOnException()
                }
                return@runCatching
            }.logOnException()
        }
    }

    private fun trackShowEvent(message: InAppMessage) {
        displayStateRepository.setDisplayed(message, Date())
        eventManager.trackInAppMessageShown(message, CONSIDER_CONSENT)
        Exponea.telemetry?.reportEvent(
            com.exponea.sdk.telemetry.model.EventType.SHOW_IN_APP_MESSAGE,
            hashMapOf("messageType" to (message.rawMessageType ?: "null"))
        )
    }

    fun processInAppMessageAction(activity: Activity, button: InAppMessagePayloadButton) {
        if (button.buttonType == DEEPLINK || button.buttonType == BROWSER) {
            try {
                activity.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        data = Uri.parse(button.buttonLink)
                    }
                )
            } catch (e: ActivityNotFoundException) {
                Logger.e(this, "Unable to perform deeplink", e)
            }
        }
    }

    override fun onEventUploaded(event: ExportedEvent) {
        val eventType = event.type
        val sdkEventType = event.sdkEventType?.let { EventType.valueOfOrNull(it) }
        if (sdkEventType != EventType.TRACK_CUSTOMER) {
            Logger.d(this, "[InApp] Event $sdkEventType is ignored for In-app fetch")
            return
        }
        Logger.d(
            this,
            "[InApp] Event $sdkEventType is uploaded to backend, going to trigger In-app handling"
        )
        val properties = event.properties?.toMap() ?: mapOf()
        val timestamp = event.timestamp ?: currentTimeSeconds()
        val customerIds = event.customerIds ?: customerIdsRepository.get().toHashMap()
        inAppShowingTriggered(sdkEventType, eventType, properties, timestamp, customerIds)
    }

    override fun onEventCreated(event: Event, type: EventType) {
        val eventType = event.type
        val properties = event.properties?.toMap() ?: mapOf()
        val timestamp = event.timestamp ?: currentTimeSeconds()
        val customerIds = event.customerIds ?: customerIdsRepository.get().toHashMap()
        if (type == EventType.TRACK_CUSTOMER) {
            Logger.d(this, "[InApp] Clearing all show requests due to customers login")
            clearPendingShowRequests()
            if (Exponea.flushMode == FlushMode.IMMEDIATE) {
                Logger.d(
                    this,
                    "[InApp] Skipping messages handling due to upcoming customer online login"
                )
                return
            }
        }
        inAppShowingTriggered(type, eventType, properties, timestamp, customerIds)
    }

    private fun clearPendingShowRequests() {
        synchronized(this) {
            pendingShowRequests = arrayListOf()
            Logger.d(this, "[InApp] Pending ShowRequests have been cleared")
        }
    }

    internal fun inAppShowingTriggered(
        type: EventType,
        eventType: String?,
        properties: Map<String, Any>,
        timestamp: Double,
        customerIds: HashMap<String, String?>
    ) {
        ensureOnBackgroundThread {
            Logger.i(
                this,
                "[InApp] Event $type:$eventType occurred, going to trigger In-app show process"
            )
            registerPendingShowRequest(eventType, properties, timestamp, customerIds)
            val eventTimestampInMillis = timestamp * 1000
            if (type == EventType.SESSION_START) {
                sessionStarted(Date(eventTimestampInMillis.toLong()))
            }
            Logger.v(this, "[InApp] Detecting reload mode for $type")
            synchronized(this) {
                when (detectReloadMode(type, eventTimestampInMillis)) {
                    ReloadMode.FETCH_AND_SHOW -> {
                        reload(customerIds, callback = { result ->
                            if (result.isFailure) {
                                Logger.e(
                                    this,
                                    "[InApp] Messages fetch failed: ${result.exceptionOrNull()?.localizedMessage}"
                                )
                            } else {
                                Logger.i(this, "[InApp] In-app message data preloaded. Picking a message to display")
                                pickAndShowMessage()
                            }
                        })
                    }
                    ReloadMode.SHOW -> {
                        Logger.d(this, "[InApp] Picking a message to display")
                        runOnBackgroundThread {
                            pickAndShowMessage()
                        }
                    }
                    ReloadMode.STOP -> {
                        Logger.w(
                            this,
                            "[InApp] Waits with message show because of running fetch"
                        )
                    }
                }
            }
        }
    }

    internal fun pickAndShowMessage() {
        val message = pickPendingMessage()
        message?.let { preloadAndShow(message.second, message.first) }
        runOnBackgroundThread {
            val otherImageUrls = loadImageUrls(
                inAppMessagesCache.get().filter { it.id != message?.second?.id }
            )
            bitmapCache.preload(otherImageUrls, callback = {
                Logger.d(this, "[InApp] Rest of images has been cached: $it")
            })
        }
    }

    internal fun preloadAndShow(message: InAppMessage, origin: InAppMessageShowRequest) {
        val imageUrls = loadImageUrls(message)
        bitmapCache.preload(imageUrls, callback = { imagesLoaded ->
            val customerIdsAfterLoad = customerIdsRepository.get().toHashMap()
            if (customerIdsAfterLoad != origin.customerIds) {
                trackError(message, "Another customer login while image load")
            } else if (imagesLoaded) {
                show(message)
            } else {
                trackError(message, "Images has not been preloaded")
            }
        })
    }

    internal fun trackError(message: InAppMessage, error: String) {
        eventManager.trackInAppMessageError(message, error, CONSIDER_CONSENT)
    }

    internal fun registerPendingShowRequest(
        eventType: String?,
        properties: Map<String, Any>,
        timestamp: Double,
        customerIds: HashMap<String, String?>
    ) {
        if (eventType == null) {
            Logger.v(this, "[InApp] Pending show request registration skipped due to no event type")
            return
        }
        Logger.i(
            this,
            "[InApp] Register request for in-app message to be shown for $eventType"
        )
        pendingShowRequests += InAppMessageShowRequest(
            eventType,
            properties,
            timestamp,
            System.currentTimeMillis(),
            customerIds
        )
    }

    override fun clear() {
        Logger.d(this, "[InApp] Clearing all data")
        inAppMessagesCache.clear()
        displayStateRepository.clear()
        clearPendingShowRequests()
    }
}

internal enum class ReloadMode {
    FETCH_AND_SHOW, SHOW, STOP
}

internal class EventManagerInAppMessageTrackingDelegate(
    context: Context,
    private val eventManager: EventManager
) : InAppMessageTrackingDelegate {
    private val deviceProperties = DeviceProperties(context)

    override fun track(
        message: InAppMessage,
        action: String,
        interaction: Boolean,
        trackingAllowed: Boolean,
        text: String?,
        link: String?,
        error: String?
    ) {
        val properties = HashMap<String, Any>()
        properties.putAll(
            hashMapOf(
            "action" to action,
            "banner_id" to message.id,
            "banner_name" to message.name,
            "banner_type" to message.messageType.value,
            "interaction" to interaction,
            "os" to "Android",
            "platform" to "Android",
            "type" to "in-app message",
            "variant_id" to message.variantId,
            "variant_name" to message.variantName
        ))
        properties.putAll(deviceProperties.toHashMap())
        if (text != null) {
            properties["text"] = text
        }
        if (link != null) {
            properties["link"] = link
        }
        error?.let { properties["error"] = it }
        if (message.consentCategoryTracking != null) {
            properties["consent_category_tracking"] = message.consentCategoryTracking
        }
        if (GdprTracking.isTrackForced(link)) {
            properties["tracking_forced"] = true
        }
        eventManager.processTrack(
            eventType = Constants.EventTypes.banner,
            properties = properties,
            type = EventType.BANNER,
            trackingAllowed = trackingAllowed
        )
    }
}

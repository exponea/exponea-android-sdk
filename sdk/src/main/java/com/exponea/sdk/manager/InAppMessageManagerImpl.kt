package com.exponea.sdk.manager

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.TrackingConsentManager.MODE.CONSIDER_CONSENT
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.DeviceProperties
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.InAppMessageButton
import com.exponea.sdk.models.InAppMessageButtonType.BROWSER
import com.exponea.sdk.models.InAppMessageButtonType.DEEPLINK
import com.exponea.sdk.models.InAppMessagePayloadButton
import com.exponea.sdk.models.InAppMessageType
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.InAppMessageBitmapCache
import com.exponea.sdk.repository.InAppMessageDisplayStateRepository
import com.exponea.sdk.repository.InAppMessagesCache
import com.exponea.sdk.services.ExponeaProjectFactory
import com.exponea.sdk.util.GdprTracking
import com.exponea.sdk.util.HtmlNormalizer
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.currentTimeSeconds
import com.exponea.sdk.util.logOnException
import com.exponea.sdk.view.InAppMessagePresenter
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class InAppMessageShowRequest(
    val eventType: String,
    val properties: Map<String, Any?>,
    val timestamp: Double?,
    val requestedAt: Long
)
internal class InAppMessageManagerImpl(
    private val configuration: ExponeaConfiguration,
    private val customerIdsRepository: CustomerIdsRepository,
    private val inAppMessagesCache: InAppMessagesCache,
    private val fetchManager: FetchManager,
    private val displayStateRepository: InAppMessageDisplayStateRepository,
    private val bitmapCache: InAppMessageBitmapCache,
    private val presenter: InAppMessagePresenter,
    private val eventManager: TrackingConsentManager,
    private val projectFactory: ExponeaProjectFactory
) : InAppMessageManager {
    companion object {
        const val REFRESH_CACHE_AFTER = 1000 * 60 * 30 // when session is started and cache is older than this, refresh
        const val MAX_PENDING_MESSAGE_AGE = 1000 * 3 // time window to show pending message after preloading
    }

    private var preloaded = false
    private var preloadInProgress = false
    private var pendingShowRequests: List<InAppMessageShowRequest> = arrayListOf()

    private var sessionStartDate = Date()

    @Synchronized
    override fun preloadIfNeeded(timestamp: Double) {
        if (shouldPreload(timestamp)) {
            preloadStarted()
            preload()
        }
    }

    override fun preload(callback: ((Result<Unit>) -> Unit)?) {
        fetchManager.fetchInAppMessages(
            exponeaProject = projectFactory.mainExponeaProject,
            customerIds = customerIdsRepository.get(),
            onSuccess = { result ->
                inAppMessagesCache.set(result.results)
                Logger.i(this, "In-app messages preloaded successfully, preloading images.")
                preloadImageAndShowPending(result.results, callback)
            },
            onFailure = {
                Logger.e(this, "Preloading in-app messages failed. ${it.results.message}")
                preloadFinished()
                showPendingMessage()
                callback?.invoke(Result.failure(Exception("Preloading in-app messages failed.")))
            }
        )
    }

    private fun preloadStarted() {
        preloaded = false
        preloadInProgress = true
    }

    private fun preloadFinished() {
        preloaded = true
        preloadInProgress = false
    }

    private fun shouldPreload(timestamp: Double): Boolean {
        if (preloadInProgress) {
            return false
        }
        return !preloaded || inAppMessagesCache.getTimestamp() + REFRESH_CACHE_AFTER < timestamp
    }

    override fun sessionStarted(sessionStartDate: Date) {
        this.sessionStartDate = sessionStartDate
    }

    private fun preloadImageAndShowPending(
        messages: List<InAppMessage>,
        callback: ((Result<Unit>) -> Unit)?
    ) = runCatching {
        bitmapCache.clearExcept(
            messages.mapNotNull { it.payload }
                .mapNotNull { it.imageUrl }
                .filter { it.isNotBlank() }
        )
        var shouldWaitWithPreload = false
        if (pendingShowRequests.isNotEmpty()) {
            Logger.i(this, "Attempt to show pending in-app message before loading all images.")
            val message = pickPendingMessage(false)
            if (message != null) {
                val imageUrls = loadImageUrls(message.second)
                if (imageUrls.isEmpty()) {
                    showPendingMessage(message)
                } else {
                    shouldWaitWithPreload = true
                    Logger.i(this, "First preload pending in-app message image, load rest later.")
                    bitmapCache.preload(imageUrls) { preloaded ->
                        if (preloaded) {
                            showPendingMessage(message)
                        } else {
                            eventManager.trackInAppMessageError(
                                message.second, "Images has not been preloaded", CONSIDER_CONSENT
                            )
                        }
                        preloadImagesAfterPendingShown(messages.filter { it != message.second }, callback)
                    }
                }
            }
        }
        if (!shouldWaitWithPreload) {
            preloadImagesAfterPendingShown(messages, callback)
        }
    }.logOnException()

    private fun preloadImagesAfterPendingShown(
        messages: List<InAppMessage>,
        callback: ((Result<Unit>) -> Unit)?
    ) = runCatching {
        val onPreloaded = {
            preloadFinished()
            showPendingMessage()
            Logger.i(this, "All in-app message images loaded.")
            callback?.invoke(Result.success(Unit))
        }
        if (messages.isEmpty()) {
            onPreloaded()
            return@runCatching
        }
        val counter = AtomicInteger(messages.size)
        for (message in messages) {
            val imageUrls = loadImageUrls(message)
            bitmapCache.preload(imageUrls) { messagePreloaded ->
                if (messagePreloaded && counter.decrementAndGet() <= 0) {
                    // this and ALL messages are preloaded
                    onPreloaded()
                }
            }
        }
    }.logOnException()

    private fun loadImageUrls(message: InAppMessage): ArrayList<String> {
        val imageURLs = ArrayList<String>()
        if (message.messageType == InAppMessageType.FREEFORM) {
            imageURLs.addAll(HtmlNormalizer(bitmapCache, message.payloadHtml!!).collectImages())
        } else if (!message.payload?.imageUrl.isNullOrEmpty()) {
            imageURLs.add(message.payload!!.imageUrl!!)
        }
        return imageURLs
    }

    private fun pickPendingMessage(requireImageLoaded: Boolean): Pair<InAppMessageShowRequest, InAppMessage>? {
        var pendingMessages: List<Pair<InAppMessageShowRequest, InAppMessage>> = arrayListOf()
        pendingShowRequests
            .filter { it.requestedAt + MAX_PENDING_MESSAGE_AGE > System.currentTimeMillis() }
            .forEach { request ->
                val messages = getFilteredMessages(
                    request.eventType,
                    request.properties,
                    request.timestamp,
                    requireImageLoaded
                )
                for (message in messages) {
                        pendingMessages += request to message
                    }
            }

        val highestPriority = pendingMessages.mapNotNull { it.second.priority }.maxOrNull() ?: 0
        pendingMessages = pendingMessages.filter { it.second.priority ?: 0 >= highestPriority }
        return if (pendingMessages.isNotEmpty()) pendingMessages.random() else null
    }

    private fun showPendingMessage(pickedMessage: Pair<InAppMessageShowRequest, InAppMessage>? = null) {
        val message = pickedMessage ?: pickPendingMessage(true)
        if (message != null) {
            show(message.second)
        }
        pendingShowRequests = arrayListOf()
    }

    private fun hasImageFor(message: InAppMessage): Boolean {
        val imageUrl = message.payload?.imageUrl
        val result = imageUrl.isNullOrEmpty() || bitmapCache.has(imageUrl)
        if (!result) {
            Logger.i(this, "Image not available for ${message.name}")
        }
        return result
    }

    override fun getFilteredMessages(
        eventType: String,
        properties: Map<String, Any?>,
        timestamp: Double?,
        requireImageLoaded: Boolean
    ): List<InAppMessage> {
        var messages = inAppMessagesCache.get()
        Logger.i(
            this,
            "Picking in-app message for eventType $eventType. " +
                "${messages.size} messages available: ${messages.map { it.name } }."
        )
        messages = messages.filter {
            (!requireImageLoaded || hasImageFor(it)) &&
                it.applyDateFilter(System.currentTimeMillis() / 1000) &&
                it.applyEventFilter(eventType, properties, timestamp) &&
                it.applyFrequencyFilter(displayStateRepository.get(it), sessionStartDate)
        }
        Logger.i(this, "${messages.size} messages available after filtering. Picking highest priority message.")
        val highestPriority = messages.mapNotNull { it.priority }.maxOrNull() ?: 0
        messages = messages.filter { it.priority ?: 0 >= highestPriority }
        Logger.i(this, "Got ${messages.size} messages with highest priority. ${messages.map { it.name } }")
        return messages
    }

    override fun getRandom(
        eventType: String,
        properties: Map<String, Any?>,
        timestamp: Double?,
        requireImageLoaded: Boolean
    ): InAppMessage? {
        val messages = getFilteredMessages(eventType, properties, timestamp, requireImageLoaded)
        if (messages.size > 1) {
            Logger.i(this, "Multiple candidate messages found, picking at random.")
        }
        return if (messages.isNotEmpty()) messages.random() else null
    }

    override fun showRandom(
        eventType: String,
        properties: Map<String, Any?>,
        timestamp: Double?
    ): Job? {
        Logger.i(this, "Requesting to show in-app message for event type $eventType")
        if (preloaded) {
            return GlobalScope.launch {
                Logger.i(this, "In-app message data preloaded, picking a message to display")
                val message = getRandom(eventType, properties, timestamp)
                if (message != null) {
                    show(message)
                }
            }
        } else {
            Logger.i(this, "Add pending in-app message to be shown after data is loaded")
            pendingShowRequests += InAppMessageShowRequest(
                eventType,
                properties,
                timestamp,
                System.currentTimeMillis()
            )
            return null
        }
    }

    private fun show(message: InAppMessage) {
        if (message.variantId == -1 && !message.hasPayload()) {
            Logger.i(this, "Only logging in-app message for control group '${message.name}'")
            trackShowEvent(message)
            return
        }
        if (!message.hasPayload()) {
            Logger.i(this, "Not showing message with empty payload '${message.name}'")
            return
        }
        Logger.i(this, "Attempting to show in-app message '${message.name}'")
        val htmlPayload: HtmlNormalizer.NormalizedResult?
        if (message.messageType == InAppMessageType.FREEFORM && !message.payloadHtml.isNullOrEmpty()) {
            htmlPayload = HtmlNormalizer(bitmapCache, message.payloadHtml).normalize()
        } else {
            htmlPayload = null
        }
        Logger.i(this, "Posting show to main thread with delay ${message.delay ?: 0}ms.")
        Handler(Looper.getMainLooper()).postDelayed(
            {
                val presented = presenter.show(
                    messageType = message.messageType,
                    payload = message.payload,
                    payloadHtml = htmlPayload,
                    timeout = message.timeout,
                    actionCallback = { activity, button ->
                        Logger.i(this, "In-app message button clicked!")
                        displayStateRepository.setInteracted(message, Date())
                        if (Exponea.inAppMessageActionCallback.trackActions) {
                            eventManager.trackInAppMessageClick(
                                message,
                                button.buttonText,
                                button.buttonLink,
                                CONSIDER_CONSENT
                            )
                        }
                        val buttonInfo = InAppMessageButton(button.buttonText, button.buttonLink)
                        Handler(Looper.getMainLooper()).post {
                            Exponea.inAppMessageActionCallback.inAppMessageAction(
                                message,
                                buttonInfo,
                                true,
                                activity
                            )
                        }
                        if (!Exponea.inAppMessageActionCallback.overrideDefaultBehavior) {
                            processInAppMessageAction(activity, button)
                        }
                    },
                    dismissedCallback = { activity ->
                        if (Exponea.inAppMessageActionCallback.trackActions) {
                            eventManager.trackInAppMessageClose(message, CONSIDER_CONSENT)
                        }
                        Handler(Looper.getMainLooper()).post {
                            Exponea.inAppMessageActionCallback.inAppMessageAction(
                                message,
                                null,
                                false,
                                activity
                            )
                        }
                    },
                    failedCallback = { error ->
                        if (Exponea.inAppMessageActionCallback.trackActions) {
                            eventManager.trackInAppMessageError(message, error, CONSIDER_CONSENT)
                        }
                    }
                )
                if (presented != null) {
                    trackShowEvent(message)
                }
            },
            message.delay ?: 0
        )
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

    override fun onEventCreated(event: Event, type: EventType) {
        val eventType = event.type
        val properties = event.properties?.toMap() ?: mapOf()
        val timestamp = event.timestamp
        val eventTimestamp = timestamp ?: currentTimeSeconds()
        val eventTimestampInMillis = eventTimestamp * 1000

        // do not initiate in-app preload on push events and session_end event
        // user may not even end up in the app and in-app fetch would run unnecessarily
        if (type != EventType.PUSH_DELIVERED &&
            type != EventType.PUSH_OPENED &&
            type != EventType.SESSION_END
        ) {
            preloadIfNeeded(eventTimestampInMillis)
        }

        if (type == EventType.SESSION_START) {
            sessionStarted(Date(eventTimestampInMillis.toLong()))
        }
        if (eventType != null) {
            showRandom(
                eventType,
                properties,
                timestamp
            )
        }
    }
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

package com.exponea.sdk.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.DeviceProperties
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.InAppMessageButtonType
import com.exponea.sdk.models.InAppMessagePayloadButton
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.InAppMessageBitmapCache
import com.exponea.sdk.repository.InAppMessageBitmapCacheImpl
import com.exponea.sdk.repository.InAppMessageDisplayStateRepository
import com.exponea.sdk.repository.InAppMessagesCache
import com.exponea.sdk.util.Logger
import com.exponea.sdk.view.InAppMessageDialogPresenter
import java.util.Date
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Until we're ready to release in-app messages, we need to be able to disable the feature completely
 */
internal class DisabledInAppMessageManagerImpl() : InAppMessageManager {
    override fun preload(callback: ((Result<Unit>) -> Unit)?) {}

    override fun getRandom(
        eventType: String,
        properties: Map<String, Any?>,
        timestamp: Double?
    ): InAppMessage? {
        return null
    }

    override fun showRandom(
        eventType: String,
        properties: Map<String, Any?>,
        timestamp: Double?,
        trackingDelegate: InAppMessageTrackingDelegate
    ): Job {
        return Job()
    }

    override fun sessionStarted(sessionStartDate: Date) {}
}

internal class InAppMessageManagerImpl(
    private val context: Context,
    private val configuration: ExponeaConfiguration,
    private val customerIdsRepository: CustomerIdsRepository,
    private val inAppMessagesCache: InAppMessagesCache,
    private val fetchManager: FetchManager,
    private val displayStateRepository: InAppMessageDisplayStateRepository,
    private val bitmapCache: InAppMessageBitmapCache = InAppMessageBitmapCacheImpl(context),
    private val presenter: InAppMessageDialogPresenter = InAppMessageDialogPresenter(context)
) : InAppMessageManager {
    private var sessionStartDate = Date()

    override fun preload(callback: ((Result<Unit>) -> Unit)?) {
        fetchManager.fetchInAppMessages(
            projectToken = configuration.projectToken,
            customerIds = customerIdsRepository.get(),
            onSuccess = { result ->
                Logger.i(this, "In-app messages preloaded successfully.")
                inAppMessagesCache.set(result.results)
                preloadImages(result.results)
                callback?.invoke(Result.success(Unit))
            },
            onFailure = {
                Logger.e(this, "Preloading in-app messages failed. ${it.results.message}")
                callback?.invoke(Result.failure(Exception("Preloading in-app messages failed.")))
            }
        )
    }

    override fun sessionStarted(sessionStartDate: Date) {
        this.sessionStartDate = sessionStartDate
    }

    private fun preloadImages(messages: List<InAppMessage>) {
        bitmapCache.clearExcept(messages.mapNotNull { it.payload.imageUrl }.filter { it.isNotBlank() })
        messages.forEach {
            if (!it.payload.imageUrl.isNullOrEmpty()) bitmapCache.preload(it.payload.imageUrl)
        }
    }

    private fun hasImageFor(message: InAppMessage): Boolean {
        return message.payload.imageUrl.isNullOrEmpty() || bitmapCache.has(message.payload.imageUrl)
    }

    override fun getRandom(eventType: String, properties: Map<String, Any?>, timestamp: Double?): InAppMessage? {
        val messages = inAppMessagesCache.get().filter {
            hasImageFor(it) &&
            it.applyDateFilter(System.currentTimeMillis() / 1000) &&
            it.applyEventFilter(eventType, properties, timestamp) &&
            it.applyFrequencyFilter(displayStateRepository.get(it), sessionStartDate)
        }
        return if (messages.isNotEmpty()) messages.random() else null
    }

    override fun showRandom(
        eventType: String,
        properties: Map<String, Any?>,
        timestamp: Double?,
        trackingDelegate: InAppMessageTrackingDelegate
    ): Job {
        return GlobalScope.launch {
            val message = getRandom(eventType, properties, timestamp)
            if (message != null) {
                val bitmap = if (!message.payload.imageUrl.isNullOrBlank())
                    bitmapCache.get(message.payload.imageUrl) ?: return@launch
                    else null
                Handler(Looper.getMainLooper()).post {
                    val presented = presenter.show(
                        messageType = message.messageType,
                        payload = message.payload,
                        image = bitmap,
                        actionCallback = { button ->
                            displayStateRepository.setInteracted(message, Date())
                            trackingDelegate.track(message, "click", true)
                            Logger.i(this, "In-app message button clicked!")
                            processInAppMessageAction(button)
                        },
                        dismissedCallback = {
                            trackingDelegate.track(message, "close", false)
                        }
                    )
                    if (presented != null) {
                        displayStateRepository.setDisplayed(message, Date())
                        trackingDelegate.track(message, "show", false)
                    }
                }
            }
        }
    }

    private fun processInAppMessageAction(button: InAppMessagePayloadButton) {
        if (button.buttonType == InAppMessageButtonType.DEEPLINK) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    data = Uri.parse(button.buttonLink)
                }
            )
        }
    }
}

internal class EventManagerInAppMessageTrackingDelegate(
    private val context: Context,
    private val eventManager: EventManager
) : InAppMessageTrackingDelegate {
    override fun track(message: InAppMessage, action: String, interaction: Boolean) {
        val properties = hashMapOf(
            "action" to action,
            "banner_id" to message.id,
            "banner_name" to message.name,
            "banner_type" to message.messageType,
            "interaction" to interaction,
            "os" to "Android",
            "type" to "in-app message",
            "variant_id" to message.variantId,
            "variant_name" to message.variantName
        )
        properties.putAll(DeviceProperties(context).toHashMap())

        eventManager.track(
            eventType = Constants.EventTypes.banner,
            properties = properties,
            type = EventType.BANNER
        )
    }
}

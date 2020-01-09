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
                Logger.e(this, "Preloading in-app messages failed.")
                callback?.invoke(Result.failure(Exception("Preloading in-app messages failed.")))
            }
        )
    }

    override fun sessionStarted(sessionStartDate: Date) {
        this.sessionStartDate = sessionStartDate
    }

    private fun preloadImages(messages: List<InAppMessage>) {
        bitmapCache.clearExcept(messages.map { it.payload.imageUrl })
        messages.forEach { bitmapCache.preload(it.payload.imageUrl) }
    }

    private fun hasImageFor(message: InAppMessage): Boolean {
        return bitmapCache.has(message.payload.imageUrl)
    }

    override fun getRandom(eventType: String): InAppMessage? {
        val messages = inAppMessagesCache.get().filter {
            hasImageFor(it) &&
            it.applyDateFilter(System.currentTimeMillis() / 1000) &&
            it.applyEventFilter(eventType) &&
            it.applyFrequencyFilter(displayStateRepository.get(it), sessionStartDate)
        }
        return if (messages.isNotEmpty()) messages.random() else null
    }

    override fun showRandom(
        eventType: String,
        trackingDelegate: InAppMessageTrackingDelegate
    ): Job {
        return GlobalScope.launch {
            val message = getRandom(eventType)
            if (message != null) {
                val bitmap = bitmapCache.get(message.payload.imageUrl) ?: return@launch
                Handler(Looper.getMainLooper()).post {
                    val presented = presenter.show(
                        payload = message.payload,
                        image = bitmap,
                        actionCallback = {
                            displayStateRepository.setInteracted(message, Date())
                            trackingDelegate.track(message, "click", true)
                            Logger.i(this, "In-app message button clicked!")
                            processInAppMessageAction(message)
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

    private fun processInAppMessageAction(message: InAppMessage) {
        if (message.payload.buttonType == "deep-link") { // there are no other actions right now, add enum later
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    data = Uri.parse(message.payload.buttonLink)
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

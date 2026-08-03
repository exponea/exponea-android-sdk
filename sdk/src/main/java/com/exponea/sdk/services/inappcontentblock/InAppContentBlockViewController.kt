package com.exponea.sdk.services.inappcontentblock

import com.exponea.sdk.Exponea
import com.exponea.sdk.models.HtmlActionType
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.models.InAppContentBlockActionType
import com.exponea.sdk.models.InAppContentBlockActionType.BROWSER
import com.exponea.sdk.models.InAppContentBlockActionType.CLOSE
import com.exponea.sdk.models.InAppContentBlockActionType.DEEPLINK
import com.exponea.sdk.models.InAppContentBlockCallback
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
import com.exponea.sdk.models.InAppContentBlockType.HTML
import com.exponea.sdk.models.InAppContentBlockType.NATIVE
import com.exponea.sdk.models.InAppContentBlockType.NOT_DEFINED
import com.exponea.sdk.models.InAppContentBlockType.UNKNOWN
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.repository.FontCache
import com.exponea.sdk.repository.HtmlNormalizedCache
import com.exponea.sdk.telemetry.model.TelemetryEvent
import com.exponea.sdk.util.ExponeaGson
import com.exponea.sdk.util.HtmlNormalizer
import com.exponea.sdk.util.HtmlNormalizer.HtmlNormalizerConfig
import com.exponea.sdk.util.HtmlNormalizer.NormalizedResult
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.logOnException
import com.exponea.sdk.util.runOnBackgroundThread
import com.exponea.sdk.util.runOnMainThread
import com.exponea.sdk.view.InAppContentBlockPlaceholderView
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal open class InAppContentBlockViewController(
    internal val placeholderId: String,
    private val config: InAppContentBlockPlaceholderConfiguration,
    private val imageCache: DrawableCache,
    private val fontCache: FontCache,
    private val htmlCache: HtmlNormalizedCache,
    private val actionDispatcher: InAppContentBlockActionDispatcher,
    private val dataLoader: InAppContentBlockDataLoader,
    internal var behaviourCallback: InAppContentBlockCallback
) {

    internal lateinit var view: InAppContentBlockPlaceholderView
    @Volatile private var isViewAttachedToWindow: Boolean = false
    private val stateLock = Any()
    private var contentLoaded: AtomicBoolean = AtomicBoolean(false)
    private val messageShown: AtomicBoolean = AtomicBoolean(false)
    private val lastTrackedMessageId: AtomicReference<String?> = AtomicReference(null)
    private val renderTrackingPending: AtomicBoolean = AtomicBoolean(false)
    private var lastRenderedNormalizedHtml: String? = null
    internal var assignedHtmlContent: NormalizedResult? = null
    internal var assignedMessage: InAppContentBlock? = null

    internal fun onUrlClick(url: String) {
        Logger.i(this, "InAppCB: Placeholder $placeholderId is clicked for action $url")
        val action = parseInAppContentBlockAction(url)
        val message = synchronized(stateLock) { assignedMessage }
        if (action == null || message == null) {
            Logger.e(
                this,
                "InAppCB: Placeholder $placeholderId has invalid state - action or message is invalid"
            )
            val errorMessage = "Invalid action definition"
            actionDispatcher.onError(placeholderId, message, errorMessage)
            kotlin.runCatching {
                behaviourCallback.onError(placeholderId, message, errorMessage)
            }.logOnException()
            return
        }
        if (action.type == CLOSE) {
            Logger.i(this, "InAppCB: Placeholder $placeholderId is closed by user")
            actionDispatcher.onClose(placeholderId, message)
            kotlin.runCatching {
                behaviourCallback.onCloseClicked(placeholderId, message)
            }.logOnException()
        } else {
            Logger.i(
                this,
                "InAppCB: Placeholder $placeholderId action detected with type ${action.type}"
            )
            actionDispatcher.onAction(placeholderId, message, action)
            kotlin.runCatching {
                behaviourCallback.onActionClicked(placeholderId, message, action)
            }.logOnException()
        }
        reloadContent()
    }

    private fun reloadContent() {
        Logger.i(this, "InAppCB: Placeholder $placeholderId is reloading for next content block")
        cleanUp()
        loadContent(true)
    }

    private fun parseInAppContentBlockAction(url: String): InAppContentBlockAction? {
        val htmlAction = synchronized(stateLock) {
            assignedHtmlContent?.findActionInfoByUrl(url)
        }
        if (htmlAction == null) {
            Logger.e(this, "InAppCB: Placeholder $placeholderId has invalid state - action is invalid")
            return null
        }
        return InAppContentBlockAction(
            type = detectActionType(htmlAction.actionType),
            name = htmlAction.buttonText,
            url = htmlAction.actionUrl
        )
    }

    private fun detectActionType(from: HtmlActionType): InAppContentBlockActionType {
        return when (from) {
            HtmlActionType.DEEPLINK -> DEEPLINK
            HtmlActionType.BROWSER -> BROWSER
            HtmlActionType.CLOSE -> CLOSE
        }
    }

    fun loadContent(requiresAttachedView: Boolean) {
        if (Exponea.isStopped) {
            Logger.e(this, "In-app content blocks UI is unavailable, SDK is stopping")
            return
        }
        renderTrackingPending.set(true)
        runOnBackgroundThread {
            Logger.i(this, "Loading InApp Content Block for placeholder $placeholderId")
            val loadedMessage = dataLoader.loadContent(placeholderId)
            val normalizedHtml = normalizeHtmlIfPossible(loadedMessage)
            val contentChanged = hasRenderableContentChanged(loadedMessage, normalizedHtml)
            if (contentLoaded.get() && messageShown.get() && !contentChanged) {
                Logger.d(
                    this,
                    "InAppCB: Placeholder $placeholderId content is unchanged, skipping redundant load"
                )
                renderTrackingPending.set(false)
                return@runOnBackgroundThread
            }
            synchronized(stateLock) {
                assignedMessage = loadedMessage
                assignedHtmlContent = normalizedHtml
            }
            contentLoaded.set(true)
            if (contentChanged) {
                messageShown.set(false)
                lastTrackedMessageId.set(null)
            }
            if (isViewAttachedToWindow || !requiresAttachedView) {
                Logger.d(
                    this,
                    """
                    InAppCB: Placeholder $placeholderId attached to window ($isViewAttachedToWindow), showing message
                    """.trimIndent()
                )
                val currentAssignedMessage = synchronized(stateLock) { assignedMessage }
                showMessage(currentAssignedMessage, allowDetachedRender = !requiresAttachedView)
            } else {
                Logger.d(
                    this,
                    "InAppCB: Placeholder $placeholderId not attached to window, will be shown on next attach"
                )
            }
        }
    }

    private fun hasRenderableContentChanged(
        loadedMessage: InAppContentBlock?,
        normalizedHtml: NormalizedResult?
    ): Boolean {
        if (!contentLoaded.get()) {
            return true
        }
        val currentMessage = synchronized(stateLock) { assignedMessage }
        if (currentMessage?.id != loadedMessage?.id) {
            return true
        }
        if (currentMessage?.contentType != loadedMessage?.contentType) {
            return true
        }
        val currentAssignedHtml = synchronized(stateLock) { assignedHtmlContent }
        return currentAssignedHtml?.html != normalizedHtml?.html
    }

    private fun normalizeHtmlIfPossible(message: InAppContentBlock?): NormalizedResult? {
        val htmlContent = message?.htmlContent
        if (message == null || message.contentType != HTML || htmlContent == null) {
            return null
        }
        Logger.d(this, "InAppCB: Normalizing HTML content of message ${message.id}")
        var normalizedHtml = htmlCache.get(message.id, htmlContent)
        if (normalizedHtml == null || !normalizedHtml.valid) {
            Logger.d(this, "InAppCB: No html cache for message ${message.id}, creating new")
            val normalizer = HtmlNormalizer(
                imageCache = imageCache,
                fontCache = fontCache,
                originalHtml = htmlContent
            )
            val normalizedResult = normalizer.normalize(HtmlNormalizerConfig(
                makeResourcesOffline = true,
                ensureCloseButton = false
            ))
            htmlCache.set(message.id, htmlContent, normalizedResult)
            normalizedHtml = normalizedResult
        } else {
            Logger.d(
                this,
                "InAppCB: Using already normalized HTML content of message ${message.id} from cache"
            )
        }
        return normalizedHtml
    }

    private fun showMessage(message: InAppContentBlock?, allowDetachedRender: Boolean = false) {
        if (!messageShown.compareAndSet(false, true)) {
            Logger.d(
                this,
                "InAppCB: Placeholder $placeholderId message already shown, skipping duplicate showMessage call"
            )
            return
        }
        if (message == null) {
            Logger.i(this, "InAppCB: No message found for placeholder $placeholderId")
            runOnMainThread {
                view.showNoContent()
            }
            kotlin.runCatching {
                behaviourCallback.onNoMessageFound(placeholderId)
            }.logOnException()
            return
        }
        Logger.i(this, "InAppCB: Message ${message.id} going to be shown for placeholder $placeholderId")
        Exponea.telemetry?.reportEvent(TelemetryEvent.CONTENT_BLOCK_SHOWN, hashMapOf(
            "type" to "static",
            "messageId" to message.id,
            "placeholders" to ExponeaGson.instance.toJson(message.placeholders)
        ))
        when (message.contentType) {
            HTML -> showHtmlContent(message, allowDetachedRender)
            NATIVE -> showNativeContent()
            UNKNOWN -> showNoContent()
            NOT_DEFINED -> showNoContent()
        }
    }

    private fun showNativeContent() {
        Logger.e(this, "InAppCB: Upgrade SDK!!! Native InApp Content Blocks are not supported here")
        // !!! Dont produce onError event
        showNoContent()
    }

    private fun showNoContent() {
        Logger.i(this, "InAppCB: No message content for placeholder $placeholderId")
        runOnMainThread {
            view.showNoContent()
        }
        val message = synchronized(stateLock) { assignedMessage }
        actionDispatcher.onNoContent(placeholderId, message)
        if (message == null) {
            Logger.i(this, "InAppCB: No message content for placeholder $placeholderId")
            runCatching {
                behaviourCallback.onNoMessageFound(placeholderId)
            }.logOnException()
        } else {
            // possibility of AB testing
            Logger.i(this, "InAppCB: Message with empty content found for placeholder $placeholderId")
            runCatching {
                behaviourCallback.onMessageShown(placeholderId, message)
            }.logOnException()
        }
    }

    private fun showHtmlContent(message: InAppContentBlock, allowDetachedRender: Boolean) {
        runOnBackgroundThread {
            if (!isViewAttachedToWindow && !allowDetachedRender) {
                Logger.d(this, "InAppCB: Placeholder $placeholderId view not attached, skipping HTML load")
                messageShown.set(false)
                return@runOnBackgroundThread
            }
            Logger.i(this, "InAppCB: Loading HTML content for placeholder $placeholderId")
            val htmlContent = message.htmlContent
            if (htmlContent.isNullOrEmpty()) {
                // possible AB testing
                Logger.i(this, "InAppCB: No HTML content provided for message ${message.id}")
                showNoContent()
                return@runOnBackgroundThread
            }
            val normalizedHtml = synchronized(stateLock) { assignedHtmlContent?.html }
            if (normalizedHtml.isNullOrEmpty()) {
                // htmlContent is non-null, normalized HTML has to be prepared; otherwise htmlContent is invalid
                Logger.e(this, "InAppCB: HTML content for message ${message.id} is invalid")
                showError(message, "Invalid HTML or empty")
                return@runOnBackgroundThread
            }
            Logger.i(this, "InAppCB: HTML content for placeholder $placeholderId is valid")
            runOnMainThread {
                if (!isViewAttachedToWindow && !allowDetachedRender) {
                    Logger.d(this, "InAppCB: Placeholder $placeholderId view detached before render, skipping")
                    messageShown.set(false)
                    return@runOnMainThread
                }
                val shouldShowExisting = synchronized(stateLock) {
                    if (lastRenderedNormalizedHtml == normalizedHtml) {
                        true
                    } else {
                        lastRenderedNormalizedHtml = normalizedHtml
                        false
                    }
                }
                if (shouldShowExisting) {
                    view.showExistingContent()
                } else {
                    view.showHtmlContent(normalizedHtml)
                }
            }
            if (lastTrackedMessageId.getAndSet(message.id) != message.id) {
                actionDispatcher.onShown(placeholderId, message)
                runCatching {
                    behaviourCallback.onMessageShown(placeholderId, message)
                }.logOnException()
            }
        }
    }

    private fun showError(message: InAppContentBlock, errorMessage: String) {
        Logger.e(
            this,
            "InAppCB: Content for placeholder $placeholderId cannot be shown because of: $errorMessage"
        )
        runOnMainThread {
            view.showNoContent()
        }
        actionDispatcher.onError(placeholderId, message, errorMessage)
        runCatching {
            behaviourCallback.onError(placeholderId, message, errorMessage)
        }.logOnException()
    }

    private fun cleanUp() {
        Logger.d(this, "InAppCB: Clean up for placeholder $placeholderId")
        contentLoaded.set(false)
        messageShown.set(false)
        lastTrackedMessageId.set(null)
        renderTrackingPending.set(false)
        synchronized(stateLock) {
            lastRenderedNormalizedHtml = null
            assignedMessage = null
            assignedHtmlContent = null
        }
    }

    fun onViewAttachedToWindow() {
        isViewAttachedToWindow = true
        if (Exponea.isStopped) {
            Logger.e(this, "In-app content blocks UI is unavailable, SDK is stopping")
            return
        }
        if (contentLoaded.get()) {
            Logger.d(
                this,
                """
                InAppCB: Placeholder $placeholderId view attached to window, content is loaded, going to show content
                """.trimIndent()
            )
            val currentAssignedMessage = synchronized(stateLock) { assignedMessage }
            showMessage(currentAssignedMessage)
        } else if (config.defferedLoad) {
            Logger.d(
                this,
                """
                InAppCB: Placeholder $placeholderId view attached to window, content needs to be loaded
                """.trimIndent()
            )
            loadContent(true)
        }
    }

    fun onViewDetachedFromWindow() {
        isViewAttachedToWindow = false
        messageShown.set(false)
        // Placeholder reset clears WebView content; drop render cache to force real reload on next attach.
        synchronized(stateLock) {
            lastRenderedNormalizedHtml = null
        }
        // For deferred-load views (carousels), holders are recycled across different blocks.
        // Resetting contentLoaded forces loadContent() on re-attach so the controller picks up
        // the new assigned block instead of showing stale content from the previous one.
        if (config.defferedLoad) {
            contentLoaded.set(false)
        }
    }

    internal fun onContentReady(contentLoaded: Boolean, finishSource: String) {
        if (!renderTrackingPending.getAndSet(false)) {
            return
        }
        val message = synchronized(stateLock) { assignedMessage }
        val messageId = message?.id
        Logger.i(
            this,
            "InAppCB: Placeholder $placeholderId render completed with contentLoaded=$contentLoaded, " +
                "finishSource=$finishSource, messageId=${messageId ?: "none"}"
        )
        val properties = hashMapOf(
            "placeholderId" to placeholderId,
            "contentLoaded" to contentLoaded.toString(),
            "finishSource" to finishSource,
            "result" to if (contentLoaded) "rendered" else "not_rendered"
        )
        messageId?.let {
            properties["messageId"] = it
        }
        Exponea.telemetry?.reportEvent(TelemetryEvent.CONTENT_BLOCK_RENDERED, properties)
    }
}

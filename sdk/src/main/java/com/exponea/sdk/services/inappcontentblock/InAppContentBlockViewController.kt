package com.exponea.sdk.services.inappcontentblock

import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.models.InAppContentBlockActionType
import com.exponea.sdk.models.InAppContentBlockActionType.BROWSER
import com.exponea.sdk.models.InAppContentBlockActionType.CLOSE
import com.exponea.sdk.models.InAppContentBlockActionType.DEEPLINK
import com.exponea.sdk.models.InAppContentBlockCallback
import com.exponea.sdk.models.InAppContentBlockFrequency.UNTIL_VISITOR_INTERACTS
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
import com.exponea.sdk.models.InAppContentBlockType.HTML
import com.exponea.sdk.models.InAppContentBlockType.NATIVE
import com.exponea.sdk.models.InAppContentBlockType.NOT_DEFINED
import com.exponea.sdk.models.InAppContentBlockType.UNKNOWN
import com.exponea.sdk.repository.BitmapCache
import com.exponea.sdk.repository.HtmlNormalizedCache
import com.exponea.sdk.repository.SimpleFileCache
import com.exponea.sdk.util.HtmlNormalizer
import com.exponea.sdk.util.HtmlNormalizer.HtmlNormalizerConfig
import com.exponea.sdk.util.HtmlNormalizer.NormalizedResult
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.runOnBackgroundThread
import com.exponea.sdk.util.runOnMainThread
import com.exponea.sdk.view.InAppContentBlockPlaceholderView

internal class InAppContentBlockViewController(
    private val placeholderId: String,
    private val config: InAppContentBlockPlaceholderConfiguration,
    private val imageCache: BitmapCache,
    private val fontCache: SimpleFileCache,
    private val htmlCache: HtmlNormalizedCache,
    private val actionDispatcher: InAppContentBlockActionDispatcher,
    private val dataLoader: InAppContentBlockDataLoader,
    internal var behaviourCallback: InAppContentBlockCallback
) {

    internal lateinit var view: InAppContentBlockPlaceholderView
    private var isViewAttachedToWindow: Boolean = false
    private var contentLoaded: Boolean = false
    private var assignedHtmlContent: NormalizedResult? = null
    private var assignedMessage: InAppContentBlock? = null

    internal fun onUrlClick(url: String) {
        Logger.d(this, "HTML InApp Content Block click for $url")
        val action = parseInAppContentBlockAction(url)
        val message = assignedMessage
        if (action == null || message == null) {
            val errorMessage = "Invalid action definition"
            actionDispatcher.onError(placeholderId, message, errorMessage)
            behaviourCallback.onError(placeholderId, message, errorMessage)
            return
        }
        if (action.type == CLOSE) {
            actionDispatcher.onClose(placeholderId, message)
            behaviourCallback.onCloseClicked(placeholderId, message)
        } else {
            actionDispatcher.onAction(placeholderId, message, action)
            behaviourCallback.onActionClicked(placeholderId, message, action)
        }
        if (message.frequency == UNTIL_VISITOR_INTERACTS) {
            Logger.i(
                this,
                "InApp Content Block ${message.id} needs to be replaced for first interaction"
            )
            reloadContent()
        }
    }

    private fun reloadContent() {
        cleanUp()
        loadContent()
    }

    private fun parseInAppContentBlockAction(url: String): InAppContentBlockAction? {
        val action = assignedHtmlContent?.findActionByUrl(url) ?: return null
        val type = detectActionType(url)
        return InAppContentBlockAction(
            type = type,
            name = action.buttonText,
            url = action.actionUrl
        )
    }

    private fun detectActionType(url: String): InAppContentBlockActionType {
        if (assignedHtmlContent?.isCloseAction(url) == true) {
            return CLOSE
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return BROWSER
        } else {
            return DEEPLINK
        }
    }

    fun loadContent() {
        runOnBackgroundThread {
            Logger.d(this, "Loading InApp Content Block for placeholder $placeholderId")
            assignedMessage = dataLoader.loadContent(placeholderId)
            contentLoaded = true
            assignedMessage?.let {
                val htmlContent = it.htmlContent
                if (it.contentType == HTML && htmlContent != null) {
                    // prepare cache
                    getOrCreateNormalizedHtml(it, htmlContent)
                }
            }
            if (isViewAttachedToWindow) {
                showMessage(assignedMessage)
            }
        }
    }

    private fun showMessage(message: InAppContentBlock?) {
        if (message == null) {
            runOnMainThread {
                view.showNoContent()
            }
            behaviourCallback.onNoMessageFound(placeholderId)
            return
        }
        Logger.i(this, "InApp Content Block ${message.id} going to be shown")
        when (message.contentType) {
            HTML -> showHtmlContent(message)
            NATIVE -> showNativeContent()
            UNKNOWN -> showNoContent()
            NOT_DEFINED -> showNoContent()
        }
    }

    private fun showNativeContent() {
        Logger.e(this, "Upgrade SDK!!! Native InApp Content Blocks are not supported here")
        // !!! Dont produce onError event
        showNoContent()
    }

    private fun showNoContent() {
        Logger.d(this, "Empty HTML InApp Content Block content")
        runOnMainThread {
            view.showNoContent()
        }
        actionDispatcher.onNoContent(placeholderId, assignedMessage)
        val message = assignedMessage
        if (message == null) {
            behaviourCallback.onNoMessageFound(placeholderId)
        } else {
            // possibility of AB testing
            behaviourCallback.onMessageShown(placeholderId, message)
        }
    }

    private fun showHtmlContent(message: InAppContentBlock) {
        runOnBackgroundThread {
            Logger.d(this, "Loading a HTML InApp Content Block content")
            val htmlContent = message.htmlContent
            if (htmlContent == null) {
                Logger.e(this, "No HTML content provided for InApp Content Block ${message.id}")
                showNoContent()
                return@runOnBackgroundThread
            }
            val normalizedHtml = getOrCreateNormalizedHtml(message, htmlContent)
            val skipHtmlShow = assignedHtmlContent?.html.contentEquals(normalizedHtml.html)
            assignedHtmlContent = normalizedHtml
            if (normalizedHtml.valid && normalizedHtml.html != null) {
                Logger.d(this, "HTML InApp Content Block loaded and showing")
                if (skipHtmlShow) {
                    Logger.d(this, "Same HTML for InApp Content Block")
                } else {
                    runOnMainThread {
                        view.showHtmlContent(normalizedHtml.html!!)
                    }
                }
                actionDispatcher.onShown(placeholderId, message)
                behaviourCallback.onMessageShown(placeholderId, message)
            } else {
                Logger.w(this, "HTML InApp Content Block has invalid payload")
                showError(message, "Invalid HTML or empty")
            }
        }
    }

    private fun showError(message: InAppContentBlock, errorMessage: String) {
        Logger.e(this, "InApp Content Block cannot be shown because of: $errorMessage")
        runOnMainThread {
            view.showNoContent()
        }
        actionDispatcher.onError(placeholderId, message, errorMessage)
        behaviourCallback.onError(placeholderId, message, errorMessage)
    }

    private fun getOrCreateNormalizedHtml(message: InAppContentBlock, htmlContent: String): NormalizedResult {
        var normalizedHtml = htmlCache.get(message.id, htmlContent)
        if (normalizedHtml == null) {
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
        }
        return normalizedHtml
    }

    private fun cleanUp() {
        contentLoaded = false
        assignedMessage = null
        assignedHtmlContent = null
    }

    fun onViewAttachedToWindow() {
        isViewAttachedToWindow = true
        if (contentLoaded) {
            showMessage(assignedMessage)
        } else if (config.defferedLoad) {
            loadContent()
        }
    }

    fun onViewDetachedFromWindow() {
        isViewAttachedToWindow = false
    }
}

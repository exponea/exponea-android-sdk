package com.exponea.sdk.services.inappcontentblock

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
    internal val placeholderId: String,
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
        Logger.i(this, "InAppCB: Placeholder $placeholderId is clicked for action $url")
        val action = parseInAppContentBlockAction(url)
        val message = assignedMessage
        if (action == null || message == null) {
            Logger.e(
                this,
                "InAppCB: Placeholder $placeholderId has invalid state - action or message is invalid"
            )
            val errorMessage = "Invalid action definition"
            actionDispatcher.onError(placeholderId, message, errorMessage)
            behaviourCallback.onError(placeholderId, message, errorMessage)
            return
        }
        if (action.type == CLOSE) {
            Logger.i(this, "InAppCB: Placeholder $placeholderId is closed by user")
            actionDispatcher.onClose(placeholderId, message)
            behaviourCallback.onCloseClicked(placeholderId, message)
        } else {
            Logger.i(
                this,
                "InAppCB: Placeholder $placeholderId action detected with type ${action.type}"
            )
            actionDispatcher.onAction(placeholderId, message, action)
            behaviourCallback.onActionClicked(placeholderId, message, action)
        }
        reloadContent()
    }

    private fun reloadContent() {
        Logger.i(this, "InAppCB: Placeholder $placeholderId is reloading for next content block")
        cleanUp()
        loadContent(true)
    }

    private fun parseInAppContentBlockAction(url: String): InAppContentBlockAction? {
        val type = detectActionType(url)
        if (type == CLOSE) {
            return InAppContentBlockAction(
                type = type,
                name = "",
                url = ""
            )
        }
        val action = assignedHtmlContent?.findActionByUrl(url) ?: return null
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

    fun loadContent(requiresAttachedView: Boolean) {
        runOnBackgroundThread {
            Logger.i(this, "Loading InApp Content Block for placeholder $placeholderId")
            assignedMessage = dataLoader.loadContent(placeholderId)
            assignedHtmlContent = normalizeHtmlIfPossible(assignedMessage)
            contentLoaded = true
            if (isViewAttachedToWindow || !requiresAttachedView) {
                Logger.d(
                    this,
                    """
                    InAppCB: Placeholder $placeholderId attached to window ($isViewAttachedToWindow), showing message
                    """.trimIndent()
                )
                showMessage(assignedMessage)
            } else {
                Logger.d(
                    this,
                    "InAppCB: Placeholder $placeholderId not attached to window, will be shown on next attach"
                )
            }
        }
    }

    private fun normalizeHtmlIfPossible(message: InAppContentBlock?): NormalizedResult? {
        val htmlContent = message?.htmlContent
        if (message == null || message.contentType != HTML || htmlContent == null) {
            return null
        }
        Logger.d(this, "InAppCB: Normalizing HTML content of message ${message.id}")
        var normalizedHtml = htmlCache.get(message.id, htmlContent)
        if (normalizedHtml == null) {
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

    private fun showMessage(message: InAppContentBlock?) {
        if (message == null) {
            Logger.i(this, "InAppCB: No message found for placeholder $placeholderId")
            runOnMainThread {
                view.showNoContent()
            }
            behaviourCallback.onNoMessageFound(placeholderId)
            return
        }
        Logger.i(this, "InAppCB: Message ${message.id} going to be shown for placeholder $placeholderId")
        when (message.contentType) {
            HTML -> showHtmlContent(message)
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
        actionDispatcher.onNoContent(placeholderId, assignedMessage)
        val message = assignedMessage
        if (message == null) {
            Logger.i(this, "InAppCB: No message content for placeholder $placeholderId")
            behaviourCallback.onNoMessageFound(placeholderId)
        } else {
            // possibility of AB testing
            Logger.i(this, "InAppCB: Message with empty content found for placeholder $placeholderId")
            behaviourCallback.onMessageShown(placeholderId, message)
        }
    }

    private fun showHtmlContent(message: InAppContentBlock) {
        runOnBackgroundThread {
            Logger.i(this, "InAppCB: Loading HTML content for placeholder $placeholderId")
            val htmlContent = message.htmlContent
            if (htmlContent.isNullOrEmpty()) {
                // possible AB testing
                Logger.i(this, "InAppCB: No HTML content provided for message ${message.id}")
                showNoContent()
                return@runOnBackgroundThread
            }
            val normalizedHtml = assignedHtmlContent?.html
            if (normalizedHtml.isNullOrEmpty()) {
                // htmlContent is non-null, normalized HTML has to be prepared; otherwise htmlContent is invalid
                Logger.e(this, "InAppCB: HTML content for message ${message.id} is invalid")
                showError(message, "Invalid HTML or empty")
                return@runOnBackgroundThread
            }
            Logger.i(this, "InAppCB: HTML content for placeholder $placeholderId is valid")
            runOnMainThread {
                view.showHtmlContent(normalizedHtml)
            }
            actionDispatcher.onShown(placeholderId, message)
            behaviourCallback.onMessageShown(placeholderId, message)
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
        behaviourCallback.onError(placeholderId, message, errorMessage)
    }

    private fun cleanUp() {
        Logger.d(this, "InAppCB: Clean up for placeholder $placeholderId")
        contentLoaded = false
        assignedMessage = null
        assignedHtmlContent = null
    }

    fun onViewAttachedToWindow() {
        isViewAttachedToWindow = true
        if (contentLoaded) {
            Logger.d(
                this,
                """
                InAppCB: Placeholder $placeholderId view attached to window, content is loaded, going to show content
                """.trimIndent()
            )
            showMessage(assignedMessage)
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
    }
}

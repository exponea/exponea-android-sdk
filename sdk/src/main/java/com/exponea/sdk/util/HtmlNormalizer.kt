package com.exponea.sdk.util

import android.graphics.Bitmap
import android.util.Base64
import androidx.annotation.WorkerThread
import com.exponea.sdk.repository.InAppMessageBitmapCache
import java.io.ByteArrayOutputStream
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import okhttp3.internal.closeQuietly
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Implementation is used by InAppMessageWebView to handle HTML inapp message paylod.
 * Original HTML is:
 * - cleaned from unwanted tags
 * - cleaned against XSS attacks
 * - cleaned from dynamic loads
 * - ensures an existence of CLOSE button
 * - transforms images into offline form
 * - ensures compatibility of action buttons
 */
internal class HtmlNormalizer(
    private var imageCache: InAppMessageBitmapCache,
    originalHtml: String
) {

    companion object {
        private const val CLOSE_ACTION_COMMAND = "close_action"
        private const val CLOSE_BUTTON_ATTR_DEF = "data-actiontype='close'"
        private const val CLOSE_BUTTON_SELECTOR = "[$CLOSE_BUTTON_ATTR_DEF]"
        private const val DATALINK_BUTTON_ATTR = "data-link"
        private const val DATALINK_BUTTON_SELECTOR = "[$DATALINK_BUTTON_ATTR]"
        private const val ANCHOR_BUTTON_SELECTOR = "a[href]"

        private const val HREF_ATTR = "href"
        private const val ANCHOR_TAG_SELECTOR = "a"
        private const val META_TAG_SELECTOR = "meta"
        private const val SCRIPT_TAG_SELECTOR = "script"
        private const val TITLE_TAG_SELECTOR = "title"
        private const val LINK_TAG_SELECTOR = "link"
        private const val IFRAME_TAG_SELECTOR = "iframe"

        /**
         * Inline javascript attributes. Listed here https://www.w3schools.com/tags/ref_eventattributes.asp
         */
        private val INLINE_SCRIPT_ATTRIBUTES = arrayOf(
                "onafterprint", "onbeforeprint", "onbeforeunload", "onerror", "onhashchange", "onload", "onmessage",
                "onoffline", "ononline", "onpagehide", "onpageshow", "onpopstate", "onresize", "onstorage", "onunload",
                "onblur", "onchange", "oncontextmenu", "onfocus", "oninput", "oninvalid", "onreset", "onsearch",
                "onselect", "onsubmit", "onkeydown", "onkeypress", "onkeyup", "onclick", "ondblclick", "onmousedown",
                "onmousemove", "onmouseout", "onmouseover", "onmouseup", "onmousewheel", "onwheel", "ondrag",
                "ondragend", "ondragenter", "ondragleave", "ondragover", "ondragstart", "ondrop", "onscroll", "oncopy",
                "oncut", "onpaste", "onabort", "oncanplay", "oncanplaythrough", "oncuechange", "ondurationchange",
                "onemptied", "onended", "onerror", "onloadeddata", "onloadedmetadata", "onloadstart", "onpause",
                "onplay", "onplaying", "onprogress", "onratechange", "onseeked", "onseeking", "onstalled", "onsuspend",
                "ontimeupdate", "onvolumechange", "onwaiting", "ontoggle"
        )

        private val ANCHOR_LINK_ATTRIBUTES = arrayOf(
            "download", "ping", "target"
        )
    }

    private var document: Document?

    init {
        try {
            document = Jsoup.parse(originalHtml)
        } catch (e: Exception) {
            Logger.i(this, "[HTML] Unable to parse original HTML source code $originalHtml")
            document = null
        }
    }

    @WorkerThread
    fun normalize(config: HtmlNormalizerConfig = DefaultConfig()): NormalizedResult {
        val result = NormalizedResult()
        if (document == null) {
            Logger.i(this, "[HTML] Original HTML code is invalid, unable to normalize")
            result.valid = false
            return result
        }
        try {
            cleanHtml(config.allowAnchorButton)
            if (config.makeImagesOffline) {
                makeImagesToBeOffline()
            }
            if (config.ensureCloseButton) {
                result.closeActionUrl = ensureCloseButton()
            }
            result.actions = ensureActionButtons(config.allowAnchorButton)
            result.html = exportHtml()
            result.valid = true
        } catch (e: Exception) {
            Logger.w(this, "[HTML] HTML parsing failed ${e.localizedMessage}")
            result.valid = false
        }
        return result
    }

    class NormalizedResult {
        var valid: Boolean = true
        var actions: List<ActionInfo>? = null
        var closeActionUrl: String? = null
        var html: String? = null
    }

    private fun cleanHtml(allowAnchorButton: Boolean) {
        // !!! Has to be called before #ensureCloseButton and #ensureActionButtons.
        if (allowAnchorButton) {
            removeAttributes(HREF_ATTR, ANCHOR_TAG_SELECTOR)
        } else {
            removeAttributes(HREF_ATTR)
        }
        ANCHOR_LINK_ATTRIBUTES.forEach {
            removeAttributes(it)
        }
        INLINE_SCRIPT_ATTRIBUTES.forEach {
            removeAttributes(it)
        }
        removeElements(META_TAG_SELECTOR)
        removeElements(SCRIPT_TAG_SELECTOR)
        removeElements(TITLE_TAG_SELECTOR)
        removeElements(LINK_TAG_SELECTOR)
        removeElements(IFRAME_TAG_SELECTOR)
    }

    private fun removeElements(selector: String) {
        val unwantedElements = document!!.select(selector)
        for (element in unwantedElements) {
            element.remove()
        }
    }

    /**
     * Removes 'href' attribute from HTML elements
     */
    private fun removeAttributes(attribute: String, skipTag: String? = null) {
        val attributedElements = document!!.select("[$attribute]")
        for (each in attributedElements) {
            if (skipTag != null && each.`is`(skipTag)) {
                continue
            }
            each.removeAttr(attribute)
        }
    }

    private fun exportHtml(): String {
        return document!!.html()
    }

    private fun ensureActionButtons(allowAnchorButton: Boolean): List<ActionInfo> {
        val result = ArrayList<ActionInfo>()
        document?.let { it ->
            if (allowAnchorButton) {
                result.addAll(collectAnchorLinkButtons(it))
            }
            result.addAll(collectDataLinkButtons(it))
        }
        return result
    }

    private fun collectAnchorLinkButtons(document: Document): List<ActionInfo> {
        val result = ArrayList<ActionInfo>()
        val actionButtons = document.select(ANCHOR_BUTTON_SELECTOR)
        for (actionButton in actionButtons) {
            val targetAction = actionButton.attr(HREF_ATTR)
            if (targetAction.isNullOrBlank()) {
                Logger.e(this, "[HTML] Action button found but with empty action")
                continue
            }
            result.add(ActionInfo(actionButton.text(), targetAction))
        }
        return result
    }

    private fun collectDataLinkButtons(document: Document): List<ActionInfo> {
        val result = ArrayList<ActionInfo>()
        val actionButtons = document.select(DATALINK_BUTTON_SELECTOR)
        for (actionButton in actionButtons) {
            val targetAction = actionButton.attr(DATALINK_BUTTON_ATTR)
            if (targetAction.isNullOrBlank()) {
                Logger.e(this, "[HTML] Action button found but with empty action")
                continue
            }
            if (!actionButton.hasParent() || !actionButton.parent()!!.`is`(ANCHOR_TAG_SELECTOR)) {
                Logger.i(this, "[HTML] Wrapping Action button with a-href")
                // randomize class name => prevents from CSS styles overriding in HTML
                val actionButtonHrefClass = "action-button-href-${UUID.randomUUID()}"
                document.head().append("<style>" +
                    ".$actionButtonHrefClass {" +
                    "    text-decoration: none;" +
                    "}" +
                    "</style>")
                actionButton.wrap("<a href='$targetAction' class='$actionButtonHrefClass'></a>")
            }
            result.add(ActionInfo(actionButton.text(), targetAction))
        }
        return result
    }

    class ActionInfo(val buttonText: String, val actionUrl: String)

    private fun ensureCloseButton(): String {
        var closeButtons = document!!.select(CLOSE_BUTTON_SELECTOR)
        if (closeButtons.isEmpty()) {
            Logger.i(this, "[HTML] Adding default close-button")
            // randomize class name => prevents from CSS styles overriding in HTML
            val closeButtonClass = "close-button-${UUID.randomUUID()}"
            document!!.body().append("<div $CLOSE_BUTTON_ATTR_DEF class='$closeButtonClass'><div>")
            document!!.head().append("""
                <style>
                    .$closeButtonClass {
                      display: inline-block;
                      position: absolute;
                      width: 20px;
                      height: 20px;
                      top: 10px;
                      right: 10px;
                      border: 2px solid #C0C0C099;
                      border-radius: 50%;
                      background-color: #FAFAFA99;
                     }
                    .$closeButtonClass:before {
                      content: '×';
                      position: absolute;
                      display: flex;
                      justify-content: center;
                      width: 20px;
                      height: 20px;
                      color: #C0C0C099;
                      font-size: 20px;
                      line-height: 20px;
                    }
                </style>
            """.trimIndent()
            )
            closeButtons = document!!.select(CLOSE_BUTTON_SELECTOR)
        }
        if (closeButtons.isEmpty()) {
            // defined or default has to exist
            throw IllegalStateException("Action close cannot be ensured")
        }
        // randomize class name => prevents from CSS styles overriding in HTML
        val closeButtonHrefClass = "close-button-href-${UUID.randomUUID()}"
        // link has to be valid URL, but is handled by String comparison anyway
        val closeActionLink = "https://exponea.com/$CLOSE_ACTION_COMMAND"
        val closeButton = closeButtons.first()!!
        if (!closeButton.hasParent() || !closeButton.parent()!!.`is`(ANCHOR_TAG_SELECTOR)) {
            Logger.i(this, "[HTML] Wrapping Close button with a-href")
            closeButton.wrap("<a href='$closeActionLink' class='$closeButtonHrefClass'></a>")
        } else if (!closeButton.parent()!!.attr("href").equals(closeActionLink, true)) {
            Logger.i(this, "[HTML] Fixing parent a-href link to close action")
            closeButton.parent()!!.attr("href", closeActionLink)
            closeButton.parent()!!.addClass(closeButtonHrefClass)
        }
        return closeActionLink
    }

    private fun makeImagesToBeOffline() {
        val images = document!!.select("img")
        for (image in images) {
            val imageSource = image.attr("src")
            if (imageSource.isNullOrEmpty()) {
                continue
            }
            try {
                image.attr("src", asBase64Image(imageSource))
            } catch (e: Exception) {
                Logger.w(this, "[HTML] Image url $imageSource has not been preloaded")
                throw e
            }
        }
    }

    private fun asBase64Image(imageSource: String): String {
        if (isBase64Uri(imageSource)) {
            return imageSource
        }
        var imageData = imageCache.get(imageSource)
        if (imageData == null) {
            val semaphore = CountDownLatch(1)
            imageCache.preload(listOf(imageSource)) { loaded ->
                if (loaded) {
                    imageData = imageCache.get(imageSource)
                }
                semaphore.countDown()
            }
            semaphore.await(10, SECONDS)
        }
        if (imageData == null) {
            Logger.e(this, "Unable to load image for HTML")
            throw IllegalStateException("Image is not preloaded")
        }
        val os = ByteArrayOutputStream()
        imageData!!.compress(Bitmap.CompressFormat.PNG, 100, os)
        val result = Base64.encodeToString(os.toByteArray(), Base64.DEFAULT)
        os.closeQuietly()
        // image type is not needed to be checked from source, WebView will fix it anyway...
        return "data:image/png;base64,$result"
    }

    /**
     * According to https://en.wikipedia.org/wiki/Data_URI_scheme#Syntax
     * data:[<media type>][;charset=<character set>][;base64],<data>
     */
    private fun isBase64Uri(uri: String): Boolean {
        return uri.startsWith("data:image/", true) && uri.contains("base64,", true)
    }

    fun collectImages(): Collection<String> {
        if (document == null) {
            return Collections.emptyList()
        }
        val target = ArrayList<String>()
        val images = document!!.select("img")
        for (imgEl in images) {
            val imageUrl = imgEl.attr("src")
            if (!imageUrl.isNullOrEmpty()) {
                target.add(imageUrl)
            }
        }
        return target
    }

    /**
     * Defines some steps for HTML normalization process
     */
    open class HtmlNormalizerConfig(
        val makeImagesOffline: Boolean,
        val ensureCloseButton: Boolean,
        val allowAnchorButton: Boolean
    )

    /**
     * Default config for HTML normalization process:
     * - Images are transformed into Base64 format
     * - Close button is ensured to be shown
     */
    private class DefaultConfig : HtmlNormalizerConfig(
        makeImagesOffline = true,
        ensureCloseButton = true,
        allowAnchorButton = false
    )
}

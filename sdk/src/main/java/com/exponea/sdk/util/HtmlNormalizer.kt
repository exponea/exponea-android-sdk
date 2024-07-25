package com.exponea.sdk.util

import android.content.Context
import android.util.Base64
import androidx.annotation.WorkerThread
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.repository.FontCacheImpl
import com.exponea.sdk.repository.InAppContentBlockBitmapCacheImpl
import com.exponea.sdk.repository.SimpleFileCache
import java.io.File
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.IGNORE_CASE
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
public class HtmlNormalizer {

    private val imageCache: DrawableCache
    private val fontCache: SimpleFileCache
    private val originalHtml: String
    private val document: Document?

    internal constructor(
        imageCache: DrawableCache,
        fontCache: SimpleFileCache,
        originalHtml: String
    ) {
        this.imageCache = imageCache
        this.fontCache = fontCache
        this.originalHtml = originalHtml
        document = try {
            Jsoup.parse(originalHtml)
        } catch (e: Exception) {
            Logger.i(this, "[HTML] Unable to parse original HTML source code $originalHtml")
            null
        }
    }

    public constructor(
        context: Context,
        originalHtml: String
    ) : this(
        InAppContentBlockBitmapCacheImpl(context),
        FontCacheImpl(context),
        originalHtml
    )

    companion object {
        private const val CLOSE_ACTION_COMMAND = "close_action"
        private const val CLOSE_BUTTON_ATTR_DEF = "data-actiontype='close'"
        private const val CLOSE_BUTTON_SELECTOR = "[$CLOSE_BUTTON_ATTR_DEF]"
        private const val DATALINK_BUTTON_ATTR = "data-link"
        private const val DATALINK_BUTTON_SELECTOR = "[$DATALINK_BUTTON_ATTR]"
        private const val ANCHOR_BUTTON_SELECTOR = "a[href]"

        private const val HREF_ATTR = "href"
        private const val ANCHOR_TAG_SELECTOR = "a"
        private const val META_TAG_SELECTOR = "meta:not([name='viewport'])"
        private const val SCRIPT_TAG_SELECTOR = "script"
        private const val TITLE_TAG_SELECTOR = "title"
        private const val LINK_TAG_SELECTOR = "link"
        private const val IFRAME_TAG_SELECTOR = "iframe"

        private const val IMAGE_MIMETYPE = "image/png"
        private const val FONT_MIMETYPE = "application/font"

        private val regExpOptions = setOf<RegexOption>(IGNORE_CASE, DOT_MATCHES_ALL)
        private val cssUrlRegexp = Regex(
            pattern = "url\\((.+?)\\)",
            options = regExpOptions
        )
        private val cssImportUrlRegexp = Regex(
            pattern = "@import[\\s]+url\\(.+?\\)",
            options = regExpOptions
        )
        private const val cssKeyFormat = "-?[_a-zA-Z]+[_a-zA-Z0-9-]*"
        private const val cssDelimiterFormat = "[\\s]*:[\\s]*"
        private const val cssValueFormat = "[^;\\n]+"
        private val cssAttributeRegexp = Regex(
            pattern = "($cssKeyFormat)$cssDelimiterFormat($cssValueFormat)",
            options = regExpOptions
        )

        /**
         * Inline javascript attributes. Listed here https://www.w3schools.com/tags/ref_eventattributes.asp
         */
        internal val INLINE_SCRIPT_ATTRIBUTES = arrayOf(
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

        internal val SUPPORTED_CSS_URL_PROPERTIES = arrayOf(
            "background", "background-image", "border-image", "border-image-source", "content", "cursor", "filter",
            "list-style", "list-style-image", "mask", "mask-image", "offset-path", "src"
        )
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
            cleanHtml()
            if (config.makeResourcesOffline) {
                makeResourcesToBeOffline()
            }
            result.actions = ensureActionButtons()
            result.closeActionUrl = detectCloseButton(config.ensureCloseButton)
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
        fun findActionByUrl(url: String): HtmlNormalizer.ActionInfo? {
            return actions?.find { URLUtils.areEqualAsURLs(it.actionUrl, url) }
        }
        fun isActionUrl(url: String?): Boolean {
            return url != null && !isCloseAction(url) && findActionByUrl(url) != null
        }
        fun isCloseAction(url: String?): Boolean {
            return url?.equals(closeActionUrl) ?: false
        }
    }

    private fun cleanHtml() {
        // !!! Has to be called before #ensureCloseButton and #ensureActionButtons.
        removeAttributes(HREF_ATTR, ANCHOR_TAG_SELECTOR)
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

    private fun ensureActionButtons(): List<ActionInfo> {
        val result = mutableMapOf<String, ActionInfo>()
        document?.let {
            // collect 'data-link' first as it may update href
            collectDataLinkButtons(it).forEach { action ->
                result[action.actionUrl] = action
            }
            collectAnchorLinkButtons(it).forEach { action ->
                result[action.actionUrl] = action
            }
        }
        return result.values.toList()
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
            if (actionButton.`is`(ANCHOR_TAG_SELECTOR)) {
                actionButton.attr(HREF_ATTR, targetAction)
            } else if (!actionButton.hasParent() || !actionButton.parent()!!.`is`(ANCHOR_TAG_SELECTOR)) {
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

    private fun detectCloseButton(ensureCloseButton: Boolean): String? {
        var closeButtons = document!!.select(CLOSE_BUTTON_SELECTOR)
        if (closeButtons.isEmpty() && ensureCloseButton) {
            Logger.i(this, "[HTML] Adding default close-button")
            // randomize class name => prevents from CSS styles overriding in HTML
            val closeButtonClass = "close-button-${UUID.randomUUID()}"
            val buttonSize = "max(min(5vw, 5vh), 16px)"
            document!!.body().append("<div $CLOSE_BUTTON_ATTR_DEF class='$closeButtonClass'><div>")
            document!!.head().append("""
                <style>
                    .$closeButtonClass {
                      display: inline-block;
                      position: absolute;
                      width: $buttonSize;
                      height: $buttonSize;
                      top: 10px;
                      right: 10px;
                      cursor: pointer;
                      border-radius: 50%;
                      background-color: rgba(250, 250, 250, 0.6);
                     }
                    .$closeButtonClass:before {
                      content: '×';
                      position: absolute;
                      display: flex;
                      justify-content: center;
                      width: $buttonSize;
                      height: $buttonSize;
                      color: rgb(0, 0, 0);
                      font-size: $buttonSize;
                      line-height: $buttonSize;
                    }
                </style>
            """.trimIndent()
            )
            closeButtons = document!!.select(CLOSE_BUTTON_SELECTOR)
        }
        if (closeButtons.isEmpty()) {
            if (ensureCloseButton) {
                // defined or default has to exist
                throw IllegalStateException("Action close cannot be ensured")
            }
            // no close button found
            return null
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

    private fun makeResourcesToBeOffline() {
        makeImageTagsToBeOffline()
        makeStylesheetsToBeOffline()
        makeStyleAttributesToBeOffline()
    }

    private fun makeStyleAttributesToBeOffline() {
        val styleAttrsElements = document!!.select("[style]")
        for (element in styleAttrsElements) {
            val styleAttr = element.attr("style")
            if (styleAttr.isNullOrEmpty()) {
                continue
            }
            element.attr("style", downloadOnlineResources(styleAttr))
        }
    }

    private fun downloadOnlineResources(styleSource: String): String {
        val onlineStatements = collectOnlineUrlStatements(styleSource)
        var styleTarget = styleSource
        for (statement in onlineStatements) {
            val dataBase64: String?
            when (statement.mimeType) {
                FONT_MIMETYPE -> dataBase64 = asBase64Font(statement.url)
                IMAGE_MIMETYPE -> dataBase64 = asBase64Image(statement.url)
                else -> {
                    dataBase64 = null
                    Logger.e(this, "Unsupported mime type ${statement.mimeType}")
                }
            }
            if (dataBase64.isNullOrBlank()) {
                Logger.e(this, "Unable to make offline resource ${statement.url}")
                continue
            }
            styleTarget = styleTarget.replace(
                statement.url, dataBase64
            )
        }
        return styleTarget
    }

    private fun collectOnlineUrlStatements(cssStyle: String): List<CssOnlineUrl> {
        val result = mutableListOf<CssOnlineUrl>()
        // CSS @import search
        val cssImportMatches = cssImportUrlRegexp.findAll(cssStyle)
        for (importRule in cssImportMatches) {
            val importUrlMatches = cssUrlRegexp.findAll(importRule.value)
            for (importUrl in importUrlMatches) {
                result.add(CssOnlineUrl(
                    mimeType = FONT_MIMETYPE,
                    url = importUrl.groupValues.last().trim('\'', '"')
                ))
            }
        }
        // CSS definitions search
        val cssDefinitionMatches = cssAttributeRegexp.findAll(cssStyle)
        for (cssDefinitionMatch in cssDefinitionMatches) {
            if (cssDefinitionMatch.groups.size != 3) {
                // skip
                continue
            }
            val cssKey = cssDefinitionMatch.groups[1]?.value
            if (cssKey == null || !SUPPORTED_CSS_URL_PROPERTIES.contains(cssKey.lowercase())) {
                // skip
                continue
            }
            val cssValue = cssDefinitionMatch.groups[2]?.value
            if (cssValue == null) {
                // skip
                continue
            }
            val urlValueMatches = cssUrlRegexp.findAll(cssValue)
            for (urlValue in urlValueMatches) {
                result.add(CssOnlineUrl(
                    mimeType = if (cssKey == "src") { FONT_MIMETYPE } else { IMAGE_MIMETYPE },
                    url = urlValue.groupValues.last().trim('\'', '"')
                ))
            }
        }
        return result
    }

    private fun makeStylesheetsToBeOffline() {
        val styles = document!!.select("style")
        for (style in styles) {
            style.text(downloadOnlineResources(style.data()))
        }
    }

    private fun makeImageTagsToBeOffline() {
        val images = document!!.select("img")
        for (image in images) {
            val imageSource = image.attr("src")
            if (imageSource.isNullOrEmpty()) {
                continue
            }
            try {
                image.attr("src", asBase64Image(imageSource))
            } catch (e: Exception) {
                Logger.w(this, "[HTML] Image url $imageSource has not been preloaded: ${e.localizedMessage}")
                throw e
            }
        }
    }

    private fun asBase64Image(imageSource: String): String {
        if (isBase64Uri(imageSource)) {
            return imageSource
        }
        val imageData = getImageFromUrl(imageSource)
        val result = Base64.encodeToString(imageData.readBytes(), Base64.NO_WRAP)
        // image type is not needed to be checked from source, WebView will fix it anyway...
        return "data:$IMAGE_MIMETYPE;base64,$result"
    }

    private fun asBase64Font(fontSource: String): String {
        if (isBase64Uri(fontSource)) {
            return fontSource
        }
        val fontData = getFileFromUrl(fontSource)
        if (fontData == null) {
            Logger.e(this, "Unable to load font $fontSource for HTML")
            return fontSource
        }
        val result = Base64.encodeToString(fontData.readBytes(), Base64.NO_WRAP)
        // font type is not needed to be precise, WebView will fix it anyway...
        return "data:$FONT_MIMETYPE;charset=utf-8;base64,$result"
    }

    private fun getFileFromUrl(url: String): File? {
        var fileData = fontCache.getFile(url)
        if (fileData == null) {
            val semaphore = CountDownLatch(1)
            fontCache.preload(listOf(url)) { loaded ->
                if (loaded) {
                    fileData = fontCache.getFile(url)
                }
                semaphore.countDown()
            }
            semaphore.await(10, SECONDS)
        }
        if (fileData == null) {
            Logger.e(this, "Unable to load file $url for HTML")
            throw IllegalStateException("File is not preloaded")
        }
        return fileData
    }

    private fun getImageFromUrl(url: String): File {
        var fileData = imageCache.getFile(url)
        if (fileData == null) {
            val semaphore = CountDownLatch(1)
            imageCache.preload(listOf(url)) { loaded ->
                if (loaded) {
                    fileData = imageCache.getFile(url)
                }
                semaphore.countDown()
            }
            semaphore.await(10, SECONDS)
        }
        if (fileData == null) {
            Logger.e(this, "Unable to load image $url for HTML")
            throw IllegalStateException("Image is not preloaded")
        }
        return fileData!!
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
        val onlineUrls = ArrayList<String>()
        // images
        val images = document!!.select("img")
        for (imgEl in images) {
            val imageUrl = imgEl.attr("src")
            if (!imageUrl.isNullOrEmpty() && !isBase64Uri(imageUrl)) {
                onlineUrls.add(imageUrl)
            }
        }
        // style tags
        val styleTags = document!!.select("style")
        for (styleTag in styleTags) {
            val styleSource = styleTag.data()
            val onlineSources = collectOnlineUrlStatements(styleSource)
            val imageOnlineSources = onlineSources.filter { it.mimeType == IMAGE_MIMETYPE }
            onlineUrls.addAll(imageOnlineSources.map { it.url })
        }
        // style attributes
        val styledEls = document!!.select("[style]")
        for (styledEl in styledEls) {
            val styleAttrSource = styledEl.attr("style")
            if (styleAttrSource.isNullOrBlank()) {
                continue
            }
            val onlineSources = collectOnlineUrlStatements(styleAttrSource)
            val imageOnlineSources = onlineSources.filter { it.mimeType == IMAGE_MIMETYPE }
            onlineUrls.addAll(imageOnlineSources.map { it.url })
        }
        // end
        return onlineUrls
    }

    /**
     * Defines some steps for HTML normalization process
     */
    open class HtmlNormalizerConfig(
        val makeResourcesOffline: Boolean,
        val ensureCloseButton: Boolean
    )

    /**
     * Default config for HTML normalization process:
     * - Images are transformed into Base64 format
     * - Close button is ensured to be shown
     */
    private class DefaultConfig : HtmlNormalizerConfig(
        makeResourcesOffline = true,
        ensureCloseButton = true
    )

    /**
     * Holds mime-type for 'url(...)' in CSS
     */
    private data class CssOnlineUrl(
        val mimeType: String,
        val url: String
    )
}

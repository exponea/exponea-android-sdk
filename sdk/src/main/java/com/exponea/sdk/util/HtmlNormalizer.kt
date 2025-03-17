package com.exponea.sdk.util

import android.content.Context
import android.util.Base64
import androidx.annotation.WorkerThread
import com.exponea.sdk.models.HtmlActionType
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.repository.DrawableCacheImpl
import com.exponea.sdk.repository.FontCache
import com.exponea.sdk.repository.FontCacheImpl
import com.exponea.sdk.repository.SimpleFileCache.Companion.DOWNLOAD_TIMEOUT_SECONDS
import java.io.File
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.IGNORE_CASE
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

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
    private val fontCache: FontCache
    private val originalHtml: String
    private val document: Document?

    internal constructor(
        imageCache: DrawableCache,
        fontCache: FontCache,
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
        DrawableCacheImpl(context),
        FontCacheImpl(context),
        originalHtml
    )

    companion object {
        private const val CLOSE_URL_PREFIX = "https://exponea.com/close_action_"
        private const val CLOSE_BUTTON_ATTR_DEF = "data-actiontype='close'"
        private const val CLOSE_BUTTON_SELECTOR = "[$CLOSE_BUTTON_ATTR_DEF]"
        private const val DATA_LINK_ATTR = "data-link"
        private const val DATA_ACTIONTYPE_ATTR = "data-actiontype"
        private const val DATA_LINK_SELECTOR = "[$DATA_LINK_ATTR]"
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
            normalizeCloseButtons(config.ensureCloseButton)
            normalizeDataLinkButtons()
            result.actions = collectAnchorLinkButtons()
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
        var actions: List<ActionInfo> = mutableListOf()
        var html: String? = null
        fun findActionInfoByUrl(url: String): ActionInfo? {
            val actionsForUrl = actions.filter { URLUtils.areEqualAsURLs(it.actionUrl, url) }
            if (actionsForUrl.isEmpty()) {
                return null
            }
            // action has priority over close
            val actionByUrl = actionsForUrl.firstOrNull { it.actionType != HtmlActionType.CLOSE }
            if (actionByUrl != null) {
                return actionByUrl
            }
            // anything else (close)
            return actionsForUrl.firstOrNull()
        }
        fun isActionUrl(url: String): Boolean {
            return findActionInfoByUrl(url)?.let { actionInfo ->
                actionInfo.actionType != HtmlActionType.CLOSE
            } ?: false
        }
        fun isCloseAction(url: String): Boolean {
            return findActionInfoByUrl(url)?.let { actionInfo ->
                actionInfo.actionType == HtmlActionType.CLOSE
            } ?: false
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

    private fun collectAnchorLinkButtons(): List<ActionInfo> {
        val result = hashMapOf<String, ActionInfo>()
        if (document == null) {
            Logger.e(this, "[HTML] Original HTML code is invalid, unable to collect buttons")
            return result.values.toList()
        }
        val actionButtons = document.select(ANCHOR_BUTTON_SELECTOR)
        for (actionButton in actionButtons) {
            val targetAction = actionButton.attr(HREF_ATTR)
            if (targetAction.isNullOrBlank()) {
                Logger.e(this, "[HTML] Action button found but with empty action")
                continue
            }
            val buttonText = readButtonText(actionButton)
            val actionType = HtmlActionType.find(actionButton.attr(DATA_ACTIONTYPE_ATTR))
                ?: determineActionTypeByUrl(targetAction)
            applyActionInfo(actionButton, targetAction, actionType)
            if (result.containsKey(targetAction)) {
                Logger.e(this, "[HTML] Action button found but with duplicate action $targetAction")
                continue
            }
            result[targetAction] = ActionInfo(
                buttonText,
                targetAction,
                actionType
            )
        }
        return result.values.toList()
    }

    /**
     * Transforms 'data-link' value into clickable <a href> form if is required.
     * Any href URL is replaced with a data-link URL.
     */
    private fun normalizeDataLinkButtons() {
        if (document == null) {
            return
        }
        val actionButtons = document.select(DATA_LINK_SELECTOR)
        for (actionButton in actionButtons) {
            val actionUrl = actionButton.attr(DATA_LINK_ATTR)
            if (actionUrl.isNullOrBlank()) {
                Logger.e(this, "[HTML] Action button found but with empty action")
                continue
            }
            val dataLinkType = HtmlActionType.find(actionButton.attr(DATA_ACTIONTYPE_ATTR))
                ?: determineActionTypeByUrl(actionUrl)
            // ensure <a href> existence
            when {
                actionButton.`is`(ANCHOR_TAG_SELECTOR) -> {
                    Logger.v(this, "[HTML] Applying data-link to an a-href link")
                    applyActionInfo(actionButton, actionUrl, dataLinkType)
                }
                actionButton.parent()?.`is`(ANCHOR_TAG_SELECTOR) == true -> {
                    Logger.v(this, "[HTML] Applying data-link to a parent as an a-href link")
                    applyActionInfo(actionButton, actionUrl, dataLinkType)
                    applyActionInfo(actionButton.parent()!!, actionUrl, dataLinkType)
                }
                else -> {
                    Logger.v(this, "[HTML] Wrapping data-link with an a-href")
                    applyActionInfo(actionButton, actionUrl, dataLinkType)
                    // randomize class name => prevents from CSS styles overriding in HTML
                    val actionButtonHrefClass = "action-button-href-${UUID.randomUUID()}"
                    wrapWithAnchorLink(
                        actionButton,
                        actionUrl,
                        actionButtonHrefClass,
                        dataLinkType
                    )
                }
            }
        }
    }

    private fun readButtonText(actionButton: Element): String? {
        var buttonText: String? = actionButton.text()
        if (buttonText.isNullOrEmpty()) {
            // Close X button produces empty string
            buttonText = null
        }
        return buttonText
    }

    private fun determineActionTypeByUrl(url: String): HtmlActionType {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return HtmlActionType.BROWSER
        } else {
            return HtmlActionType.DEEPLINK
        }
    }

    class ActionInfo(
        val buttonText: String?,
        val actionUrl: String,
        val actionType: HtmlActionType
    )

    /**
     * Transforms elements with 'data-actiontype="close"' into clickable <a href> form if is required.
     * Any existing href URL is replaced with a close URL.
     */
    private fun normalizeCloseButtons(ensureCloseButton: Boolean) {
        if (document == null) {
            return
        }
        var closeButtons = document.select(CLOSE_BUTTON_SELECTOR)
        if (closeButtons.isEmpty() && ensureCloseButton) {
            Logger.i(this, "[HTML] Adding default close-button")
            // randomize class name => prevents from CSS styles overriding in HTML
            val closeButtonClass = "close-button-${UUID.randomUUID()}"
            val buttonSize = "max(min(5vw, 5vh), 16px)"
            document.body().append("<div $CLOSE_BUTTON_ATTR_DEF class='$closeButtonClass'><div>")
            document.head().append("""
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
            closeButtons = document.select(CLOSE_BUTTON_SELECTOR)
        }
        if (ensureCloseButton && closeButtons.isEmpty()) {
            // defined or default has to exist
            throw IllegalStateException("Action close cannot be ensured")
        }
        closeButtons.forEach { closeButton ->
            val closeButtonId = UUID.randomUUID().toString()
            val closeButtonUrl = "$CLOSE_URL_PREFIX$closeButtonId"
            // ensure <a href> existence
            when {
                closeButton.`is`(ANCHOR_TAG_SELECTOR) -> {
                    Logger.i(this, "[HTML] Fixing close button as a-href link to close action")
                    applyActionInfo(closeButton, closeButtonUrl, HtmlActionType.CLOSE)
                }
                closeButton.parent()?.`is`(ANCHOR_TAG_SELECTOR) == true -> {
                    Logger.i(this, "[HTML] Fixing parent a-href link to close action")
                    applyActionInfo(closeButton, closeButtonUrl, HtmlActionType.CLOSE)
                    applyActionInfo(closeButton.parent()!!, closeButtonUrl, HtmlActionType.CLOSE)
                }
                else -> {
                    Logger.i(this, "[HTML] Wrapping Close button with an a-href")
                    applyActionInfo(closeButton, closeButtonUrl, HtmlActionType.CLOSE)
                    // randomize class name => prevents from CSS styles overriding in HTML
                    val closeButtonHrefClass = "close-button-href-$closeButtonId"
                    wrapWithAnchorLink(
                        closeButton,
                        closeButtonUrl,
                        closeButtonHrefClass,
                        HtmlActionType.CLOSE
                    )
                }
            }
        }
    }

    private fun applyActionInfo(target: Element, url: String, dataActionType: HtmlActionType) {
        if (target.`is`(ANCHOR_TAG_SELECTOR)) {
            target.attr("href", url)
        }
        target.attr(DATA_LINK_ATTR, url)
        target.attr(DATA_ACTIONTYPE_ATTR, dataActionType.value)
    }

    private fun wrapWithAnchorLink(
        child: Element,
        href: String,
        cssClass: String,
        dataActionType: HtmlActionType
    ) {
        document?.head()?.append("""
            <style>
            .$cssClass {
              text-decoration: none;
            }
            </style>
        """.trimIndent())
        child.wrap("""
        <a href='$href'
            class='$cssClass'
            $DATA_LINK_ATTR='$href'
            $DATA_ACTIONTYPE_ATTR='${dataActionType.value}'
            >
        </a>
        """.trimIndent())
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
        var fileData = fontCache.getFontFile(url)
        if (fileData == null) {
            val semaphore = CountDownLatch(1)
            fontCache.preload(listOf(url)) {
                semaphore.countDown()
            }
            semaphore.await(DOWNLOAD_TIMEOUT_SECONDS, SECONDS)
            fileData = fontCache.getFontFile(url)
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
            imageCache.preload(listOf(url)) {
                semaphore.countDown()
            }
            semaphore.await(DOWNLOAD_TIMEOUT_SECONDS, SECONDS)
            fileData = imageCache.getFile(url)
        }
        if (fileData == null) {
            Logger.e(this, "Unable to load image $url for HTML")
            throw IllegalStateException("Image is not preloaded")
        }
        return fileData
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
        val images = document.select("img")
        for (imgEl in images) {
            val imageUrl = imgEl.attr("src")
            if (imageUrl.isNotBlank() && !isBase64Uri(imageUrl)) {
                onlineUrls.add(imageUrl)
            }
        }
        // style tags
        val styleTags = document.select("style")
        for (styleTag in styleTags) {
            val styleSource = styleTag.data()
            val onlineSources = collectOnlineUrlStatements(styleSource)
            val imageOnlineSources = onlineSources.filter { it.mimeType == IMAGE_MIMETYPE }
            onlineUrls.addAll(imageOnlineSources.map { it.url }.filter { it.isNotBlank() })
        }
        // style attributes
        val styledEls = document.select("[style]")
        for (styledEl in styledEls) {
            val styleAttrSource = styledEl.attr("style")
            if (styleAttrSource.isNullOrBlank()) {
                continue
            }
            val onlineSources = collectOnlineUrlStatements(styleAttrSource)
            val imageOnlineSources = onlineSources.filter { it.mimeType == IMAGE_MIMETYPE }
            onlineUrls.addAll(imageOnlineSources.map { it.url }.filter { it.isNotBlank() })
        }
        // end
        return onlineUrls
    }

    fun collectFonts(): Collection<String> {
        if (document == null) {
            return Collections.emptyList()
        }
        val onlineUrls = ArrayList<String>()
        // style tags
        val styleTags = document.select("style")
        for (styleTag in styleTags) {
            val styleSource = styleTag.data()
            val onlineSources = collectOnlineUrlStatements(styleSource)
            val fontOnlineSources = onlineSources.filter { it.mimeType == FONT_MIMETYPE }
            onlineUrls.addAll(fontOnlineSources.map { it.url }.filter { it.isNotBlank() })
        }
        // style attributes
        val styledEls = document.select("[style]")
        for (styledEl in styledEls) {
            val styleAttrSource = styledEl.attr("style")
            if (styleAttrSource.isNullOrBlank()) {
                continue
            }
            val onlineSources = collectOnlineUrlStatements(styleAttrSource)
            val fontOnlineSources = onlineSources.filter { it.mimeType == FONT_MIMETYPE }
            onlineUrls.addAll(fontOnlineSources.map { it.url }.filter { it.isNotBlank() })
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

package com.exponea.sdk.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.RelativeLayout
import androidx.cardview.widget.CardView
import com.exponea.sdk.Exponea
import com.exponea.sdk.R
import com.exponea.sdk.databinding.InappContentBlockPlaceholderBinding
import com.exponea.sdk.models.InAppContentBlockCallback
import com.exponea.sdk.services.OnIntegrationStoppedCallback
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockViewController
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.ensureOnBackgroundThread
import com.exponea.sdk.util.ensureOnMainThread
import com.exponea.sdk.util.logOnException

@SuppressLint("ViewConstructor")
class InAppContentBlockPlaceholderView internal constructor(
    context: Context,
    internal val controller: InAppContentBlockViewController
) : RelativeLayout(context, null, 0), OnIntegrationStoppedCallback {

    internal lateinit var htmlContainer: ExponeaWebView
    internal lateinit var placeholder: CardView
    private var onContentReady: ((Boolean) -> Unit)? = null
    private var onHeightUpdate: ((Int) -> Unit)? = null
    private val placeholderId: String = controller.placeholderId
    internal var isReadyForHeightMeasurement: Boolean = false
        private set

    /**
     * Whenever a in-app content block message is handled, this callback is called, if set up.
     * Otherwise default behaviour is handled by the SDK
     */
    @Suppress("RedundantVisibilityModifier")
    public var behaviourCallback: InAppContentBlockCallback
        get() {
            return controller.behaviourCallback
        }
        set(value) {
            controller.behaviourCallback = value
        }

    init {
        controller.view = this
        inflateLayout()
        registerHandlers()
        Logger.v(this, "InAppCB: $placeholderId: View initialized")
    }

    private fun registerHandlers() {
        htmlContainer.setOnUrlCallback { url ->
            Logger.v(this, "InAppCB: $placeholderId: URL $url clicked")
            controller.onUrlClick(url)
        }
        htmlContainer.setOnPageLoadedCallback { finishSource ->
            Logger.v(
                this,
                "InAppCB: $placeholderId: HTML content has been fully loaded with finishSource=$finishSource"
            )
            notifyContentReadyListener(true, ContentReadyFinishSourceValue(finishSource))
        }
        this@InAppContentBlockPlaceholderView.addOnLayoutChangeListener { viewInstance, _, _, _, _, _, _, _, _ ->
            Logger.v(this, "InAppCB: $placeholderId: View layout changed")
            if (!isReadyForHeightMeasurement) {
                Logger.v(this, "InAppCB: $placeholderId: Skipping height update, content is not ready")
                return@addOnLayoutChangeListener
            }
            onHeightUpdate?.let {
                kotlin.runCatching {
                    it.invoke(viewInstance.height)
                }.logOnException()
            }
        }
    }

    private fun notifyContentReadyListener(
        contentLoaded: Boolean,
        finishSource: ContentReadyFinishSourceValue
    ) {
        Logger.i(
            this,
            "InAppCB: $placeholderId: Page loaded, notifying content ready with $contentLoaded, " +
                "finishSource=${finishSource.value}"
        )
        isReadyForHeightMeasurement = true
        controller.onContentReady(contentLoaded, finishSource.value)
        onContentReady?.let {
            kotlin.runCatching {
                it.invoke(contentLoaded)
            }.logOnException()
        }
    }

    private fun inflateLayout() {
        val viewBinding = InappContentBlockPlaceholderBinding.bind(
            View.inflate(context, R.layout.inapp_content_block_placeholder, this)
        )
        this.htmlContainer = viewBinding.contentBlockWebview
        this.htmlContainer.setBackgroundColor(Color.TRANSPARENT)
        this.placeholder = viewBinding.contentBlockPlaceholder
        applyVisibilityMode(PlaceholderVisibilityMode.INIT)
    }

    private fun applyVisibilityMode(mode: PlaceholderVisibilityMode) {
        when (mode) {
            PlaceholderVisibilityMode.INIT -> {
                this.visibility = VISIBLE
                this.htmlContainer.visibility = GONE
                this.placeholder.visibility = GONE
            }
            PlaceholderVisibilityMode.EMPTY -> {
                this.htmlContainer.visibility = GONE
                this.placeholder.visibility = VISIBLE
                if (mayHaveZeroSizeForEmptyContent()) {
                    this.visibility = GONE
                } else {
                    this.visibility = VISIBLE
                }
            }
            PlaceholderVisibilityMode.CONTENT -> {
                this.visibility = VISIBLE
                this.htmlContainer.visibility = VISIBLE
                this.placeholder.visibility = GONE
            }
        }
    }

    internal fun showNoContent() {
        Logger.i(this, "InAppCB: Placeholder ${controller.placeholderId} view has no content to show")
        applyVisibilityMode(PlaceholderVisibilityMode.EMPTY)
        notifyContentReadyListener(false, ContentReadyFinishSource.EMPTY)
    }

    private fun mayHaveZeroSizeForEmptyContent(): Boolean {
        if (minimumHeight > 0 && minimumWidth > 0) {
            return false
        }
        val layoutParams = this.layoutParams ?: return true
        return layoutParams.height == LayoutParams.WRAP_CONTENT ||
            layoutParams.width == LayoutParams.WRAP_CONTENT
    }

    override fun onAttachedToWindow() {
        Logger.d(
            this,
            "InAppCB: $placeholderId: View has been attached to window"
        )
        super.onAttachedToWindow()
        Exponea.deintegration.registerForIntegrationStopped(this)
        controller.onViewAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        Logger.d(
            this,
            "InAppCB: $placeholderId: View has been detached from window"
        )
        controller.onViewDetachedFromWindow()
        Exponea.deintegration.unregisterForIntegrationStopped(this)
        super.onDetachedFromWindow()
    }

    internal fun showHtmlContent(html: String) {
        Logger.i(this, "InAppCB: $placeholderId: View going to show HTML block")
        isReadyForHeightMeasurement = false
        htmlContainer.loadData(html)
        applyVisibilityMode(PlaceholderVisibilityMode.CONTENT)
    }

    internal fun showExistingContent() {
        Logger.d(this, "InAppCB: $placeholderId: Reusing already-rendered content, skipping WebView reload")
        isReadyForHeightMeasurement = true
        applyVisibilityMode(PlaceholderVisibilityMode.CONTENT)
        notifyContentReadyListener(true, ContentReadyFinishSource.EXISTING)
    }

    fun refreshContent() {
        Logger.i(this, "InAppCB: $placeholderId: View requested to be refreshed")
        controller.loadContent(false)
    }

    /**
     * Triggers a load respecting the ETag cache.
     * Use for the initial trigger and on screen re-appearance (e.g. onResume).
     * Use [reload] only when an explicit force-refresh is required (e.g. pull-to-refresh).
     */
    fun load() {
        if (Exponea.isStopped) {
            Logger.e(this, "In-app content blocks UI is unavailable, SDK is stopping")
            return
        }
        Logger.i(this, "InAppCB: $placeholderId: load() requested")
        ensureOnBackgroundThread {
            val manager = Exponea.getComponent()?.inAppContentBlockManager ?: run {
                Logger.e(this, "InAppCB: $placeholderId: SDK not initialized")
                return@ensureOnBackgroundThread
            }
            val contentBlocks = manager.getAllInAppContentBlocksForPlaceholder(placeholderId)
            manager.loadContentIfNeededSync(contentBlocks, forceRefresh = false)
            controller.loadContent(false)
        }
    }

    /**
     * Forces an unconditional re-fetch ignoring any cached ETag.
     * Use for explicit user actions such as pull-to-refresh.
     * For regular re-checks on screen re-appearance use [load] instead.
     */
    fun reload() {
        if (Exponea.isStopped) {
            Logger.e(this, "In-app content blocks UI is unavailable, SDK is stopping")
            return
        }
        Logger.i(this, "InAppCB: $placeholderId: reload() requested — force refresh")
        ensureOnBackgroundThread {
            val manager = Exponea.getComponent()?.inAppContentBlockManager ?: run {
                Logger.e(this, "InAppCB: $placeholderId: SDK not initialized")
                return@ensureOnBackgroundThread
            }
            val contentBlocks = manager.getAllInAppContentBlocksForPlaceholder(placeholderId)
            manager.loadContentIfNeededSync(contentBlocks, forceRefresh = true)
            controller.loadContent(false)
        }
    }

    internal fun resetContent() {
        isReadyForHeightMeasurement = false
        htmlContainer.clearContent()
        applyVisibilityMode(PlaceholderVisibilityMode.INIT)
    }

    fun setOnContentReadyListener(listener: (Boolean) -> Unit) {
        onContentReady = listener
    }

    fun setOnHeightUpdateListener(listener: (Int) -> Unit) {
        onHeightUpdate = listener
    }

    fun invokeActionClick(actionUrl: String) {
        Logger.i(
            this,
            "InAppCB: $placeholderId: Manual action $actionUrl invoked"
        )
        controller.onUrlClick(actionUrl)
    }

    private enum class PlaceholderVisibilityMode {
        INIT, EMPTY, CONTENT
    }

    private enum class ContentReadyFinishSource(val value: String) {
        EXISTING("existing"),
        EMPTY("empty")
    }

    private data class ContentReadyFinishSourceValue(val value: String)

    private fun notifyContentReadyListener(
        contentLoaded: Boolean,
        finishSource: ContentReadyFinishSource
    ) {
        notifyContentReadyListener(contentLoaded, ContentReadyFinishSourceValue(finishSource.value))
    }

    override fun onIntegrationStopped() {
        ensureOnMainThread { visibility = GONE }
    }
}

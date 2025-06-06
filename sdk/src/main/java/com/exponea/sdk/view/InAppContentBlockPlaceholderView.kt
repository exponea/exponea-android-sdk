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
import com.exponea.sdk.util.ThreadSafeAccess
import com.exponea.sdk.util.logOnException
import com.exponea.sdk.util.runOnBackgroundThread
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Job

@SuppressLint("ViewConstructor")
class InAppContentBlockPlaceholderView internal constructor(
    context: Context,
    internal val controller: InAppContentBlockViewController
) : RelativeLayout(context, null, 0), OnIntegrationStoppedCallback {

    private companion object {
        private const val CONTENT_READY_TIMEOUT = 200L
    }

    internal lateinit var htmlContainer: ExponeaWebView
    internal lateinit var placeholder: CardView
    private var onContentReady: ((Boolean) -> Unit)? = null
    private var onHeightUpdate: ((Int) -> Unit)? = null
    private val contentLoadedFlag = AtomicReference<Boolean?>(null)
    private var contentLoadedForceUpdate: Job? = null
    private val jobAccess = ThreadSafeAccess()
    private val placeholderId: String = controller.placeholderId

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
        htmlContainer.setOnPageLoadedCallback {
            Logger.v(this, "InAppCB: $placeholderId: HTML content has been fully loaded")
            startNotifyContentReadyProcess(true)
        }
        this@InAppContentBlockPlaceholderView.addOnLayoutChangeListener { viewInstance, _, _, _, _, _, _, _, _ ->
            Logger.v(this, "InAppCB: $placeholderId: View layout changed")
            // layout change should notify only if content was loaded
            contentLoadedFlag.getAndSet(null)?.let { contentLoaded ->
                jobAccess.waitForAccess {
                    contentLoadedForceUpdate?.cancel()
                    contentLoadedForceUpdate = null
                }
                Logger.v(this, "InAppCB: $placeholderId: Finishing NotifyContentReadyProcess after layout change")
                notifyContentReadyListener(contentLoaded)
            }
            onHeightUpdate?.let {
                kotlin.runCatching {
                    it.invoke(viewInstance.height)
                }.logOnException()
            }
        }
    }

    private fun notifyContentReadyListener(contentLoaded: Boolean) {
        Logger.i(this, "InAppCB: $placeholderId: Page loaded, notifying content ready with $contentLoaded")
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
        startNotifyContentReadyProcess(false)
        applyVisibilityMode(PlaceholderVisibilityMode.EMPTY)
    }

    private fun startNotifyContentReadyProcess(contentLoaded: Boolean) {
        contentLoadedFlag.set(contentLoaded)
        // OnLayoutChangeListener should be called in near future or force to notify onContentReady
        jobAccess.waitForAccess {
            contentLoadedForceUpdate?.cancel()
            contentLoadedForceUpdate = runOnBackgroundThread(CONTENT_READY_TIMEOUT) {
                jobAccess.waitForAccess {
                    contentLoadedForceUpdate = null
                }
                Logger.v(this, "InAppCB: $placeholderId: Force-notifying content ready listener")
                notifyContentReadyListener(contentLoadedFlag.getAndSet(null) ?: false)
            }
        }
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
        htmlContainer.loadData(html)
        applyVisibilityMode(PlaceholderVisibilityMode.CONTENT)
    }

    fun refreshContent() {
        Logger.i(this, "InAppCB: $placeholderId: View requested to be refreshed")
        controller.loadContent(false)
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

    override fun onIntegrationStopped() {
        visibility = GONE
    }
}

package com.exponea.sdk.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.RelativeLayout
import androidx.cardview.widget.CardView
import com.exponea.sdk.R
import com.exponea.sdk.models.InAppContentBlockCallback
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockViewController
import java.util.concurrent.atomic.AtomicReference
import kotlinx.android.synthetic.main.inapp_content_block_placeholder.view.content_block_placeholder
import kotlinx.android.synthetic.main.inapp_content_block_placeholder.view.content_block_webview

@SuppressLint("ViewConstructor")
class InAppContentBlockPlaceholderView internal constructor(
    context: Context,
    internal val controller: InAppContentBlockViewController
) : RelativeLayout(context, null, 0) {

    private lateinit var htmlContainer: ExponeaWebView
    private lateinit var placeholder: CardView
    private var onContentReady: ((Boolean) -> Unit)? = null
    private val pageFinishedEvent = AtomicReference<Boolean?>(null)
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
    }

    private fun registerHandlers() {
        htmlContainer.setOnUrlCallback { url ->
            controller.onUrlClick(url)
        }
        htmlContainer.setOnPageLoadedCallback {
            pageFinishedEvent.set(true)
        }
        this@InAppContentBlockPlaceholderView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            pageFinishedEvent.getAndSet(null)?.let { contentLoaded ->
                onContentReady?.invoke(contentLoaded)
            }
        }
    }

    private fun inflateLayout() {
        View.inflate(context, R.layout.inapp_content_block_placeholder, this)
        this.htmlContainer = this.content_block_webview
        this.htmlContainer.setBackgroundColor(Color.TRANSPARENT)
        // all modes has to be hidden before usage
        this.htmlContainer.visibility = GONE
        this.placeholder = this.content_block_placeholder
        this.placeholder.visibility = GONE
    }

    internal fun showNoContent() {
        pageFinishedEvent.set(false)
        if (mayHaveZeroSizeForEmptyContent()) {
            this.visibility = GONE
        } else {
            this.placeholder.visibility = VISIBLE
        }
    }

    private fun mayHaveZeroSizeForEmptyContent(): Boolean {
        if (minimumHeight > 0 && minimumWidth > 0) {
            return false
        }
        return this.layoutParams.height == LayoutParams.WRAP_CONTENT ||
            this.layoutParams.width == LayoutParams.WRAP_CONTENT
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        controller.onViewAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        controller.onViewDetachedFromWindow()
        super.onDetachedFromWindow()
    }

    internal fun showHtmlContent(html: String) {
        htmlContainer.loadData(html)
        // pageFinishedEvent will be set after html full load
        htmlContainer.visibility = VISIBLE
    }

    fun refreshContent() {
        controller.loadContent()
    }

    fun setOnContentReadyListener(listener: (Boolean) -> Unit) {
        onContentReady = listener
    }
}

package com.exponea.sdk.view

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.RelativeLayout
import androidx.cardview.widget.CardView
import com.exponea.sdk.R
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockViewController
import java.util.concurrent.atomic.AtomicReference
import kotlinx.android.synthetic.main.inapp_content_block_placeholder.view.content_block_placeholder
import kotlinx.android.synthetic.main.inapp_content_block_placeholder.view.content_block_webview

public class InAppContentBlockPlaceholderView constructor(
    context: Context
) : RelativeLayout(context, null, 0) {

    internal var controller: InAppContentBlockViewController? = null
    private lateinit var htmlContainer: ExponeaWebView
    private lateinit var placeholder: CardView
    private var onContentReady: ((Boolean) -> Unit)? = null
    private val pageFinishedEvent = AtomicReference<Boolean?>(null)

    init {
        inflateLayout()
        registerHandlers()
    }

    private fun registerHandlers() {
        htmlContainer.setOnUrlCallback { url ->
            controller?.onUrlClick(url, context)
        }
        htmlContainer.setOnPageLoadedCallback {
            pageFinishedEvent.set(true)
        }
        this.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
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

    public fun showNoContent() {
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
        controller?.onViewAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        controller?.onViewDetachedFromWindow()
        super.onDetachedFromWindow()
    }

    fun showHtmlContent(html: String) {
        htmlContainer.loadData(html)
        // pageFinishedEvent will be set after html full load
        htmlContainer.visibility = VISIBLE
    }

    public fun refreshContent() {
        controller?.loadContent()
    }

    fun setOnContentReadyListener(listener: (Boolean) -> Unit) {
        onContentReady = listener
    }
}

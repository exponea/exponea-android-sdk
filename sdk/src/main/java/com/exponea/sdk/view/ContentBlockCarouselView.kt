package com.exponea.sdk.view

import android.content.ComponentName
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.exponea.sdk.R
import com.exponea.sdk.models.ContentBlockCarouselCallback
import com.exponea.sdk.models.ContentBlockSelector
import com.exponea.sdk.services.inappcontentblock.ContentBlockCarouselViewController
import com.exponea.sdk.services.inappcontentblock.ContentBlockCarouselViewController.Companion.DEFAULT_MAX_MESSAGES_COUNT
import com.exponea.sdk.services.inappcontentblock.ContentBlockCarouselViewController.Companion.DEFAULT_SCROLL_DELAY
import com.exponea.sdk.services.inappcontentblock.ContentBlockCarouselViewController.Companion.EMPTY_PLACEHOLDER_ID
import com.exponea.sdk.services.inappcontentblock.ContentBlockCarouselViewHolder
import com.exponea.sdk.util.Logger
import kotlinx.android.synthetic.main.inapp_content_block_carousel.view.content_block_carousel_pager

public class ContentBlockCarouselView : RelativeLayout {

    public var behaviourCallback: ContentBlockCarouselCallback?
        get() = viewController.behaviourCallback
        set(value) {
            viewController.behaviourCallback = value
        }

    public var contentBlockSelector: ContentBlockSelector
        get() = viewController.contentBlockSelector
        set(value) {
            viewController.contentBlockSelector = value
        }

    private lateinit var viewPager: ViewPager2
    internal lateinit var viewController: ContentBlockCarouselViewController

    private var tabsClient: CustomTabsClient? = null
    private var tabsSession: CustomTabsSession? = null
    private val tabsCallback = object : CustomTabsCallback() {
        override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
            when (navigationEvent) {
                NAVIGATION_FAILED,
                TAB_HIDDEN -> {
                    Logger.i(this, "InAppCbCarousel: User returns to app")
                    viewController.onViewBecomeForeground()
                }
                else -> {
                    Logger.v(this, "InAppCbCarousel: Web Navigation event: $navigationEvent")
                }
            }
        }
    }
    private val tabsConnection = object : CustomTabsServiceConnection() {
        override fun onServiceDisconnected(name: ComponentName?) {
            tabsClient = null
            tabsSession = null
        }

        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            tabsClient = client
            tabsClient?.warmup(0)
            tabsSession = tabsClient?.newSession(tabsCallback)
        }
    }

    // Xml UI constructors
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(
            context,
            readPlaceholderId(context, attrs, defStyleAttr, 0),
            readMaxMessagesCount(context, attrs, defStyleAttr, 0),
            readScrollDelay(context, attrs, defStyleAttr, 0)
        )
    }
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, sAttrs: Int, sRes: Int) : super(context, attrs, sAttrs, sRes) {
        init(
            context,
            readPlaceholderId(context, attrs, sAttrs, sRes),
            readMaxMessagesCount(context, attrs, sAttrs, sRes),
            readScrollDelay(context, attrs, sAttrs, sRes)
        )
    }
    // Dynamic UI constructors
    constructor(
        context: Context,
        placeholderId: String,
        maxMessagesCount: Int = DEFAULT_MAX_MESSAGES_COUNT,
        scrollDelay: Int = DEFAULT_SCROLL_DELAY
    ) :
        this(context, placeholderId, maxMessagesCount, scrollDelay, null)
    constructor(
        context: Context,
        placeholderId: String,
        maxMessagesCount: Int = DEFAULT_MAX_MESSAGES_COUNT,
        scrollDelay: Int = DEFAULT_SCROLL_DELAY,
        attrs: AttributeSet?
    ) :
        this(context, placeholderId, maxMessagesCount, scrollDelay, attrs, 0)
    constructor(
        context: Context,
        placeholderId: String,
        maxMessagesCount: Int = DEFAULT_MAX_MESSAGES_COUNT,
        scrollDelay: Int = DEFAULT_SCROLL_DELAY,
        attrs: AttributeSet?,
        sAttrs: Int
    ) :
        this(context, attrs, sAttrs) {
        init(context, placeholderId, maxMessagesCount, scrollDelay)
    }
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context,
        placeholderId: String,
        maxMessagesCount: Int = DEFAULT_MAX_MESSAGES_COUNT,
        scrollDelay: Int = DEFAULT_SCROLL_DELAY,
        attrs: AttributeSet?,
        sAttrs: Int,
        sRes: Int
    ) : this(context, attrs, sAttrs, sRes) {
        init(context, placeholderId, maxMessagesCount, scrollDelay)
    }
    private fun readPlaceholderId(context: Context, attrs: AttributeSet?, defStyleAttr: Int, sRes: Int): String {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ContentBlockCarouselView, defStyleAttr, sRes)
        val placeholderId = typedArray.getString(R.styleable.ContentBlockCarouselView_placeholderId)
        typedArray.recycle()
        return placeholderId ?: EMPTY_PLACEHOLDER_ID
    }
    private fun readMaxMessagesCount(context: Context, attrs: AttributeSet?, defStyleAttr: Int, sRes: Int): Int {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ContentBlockCarouselView, defStyleAttr, sRes)
        val maxMessagesCount = typedArray.getInt(
            R.styleable.ContentBlockCarouselView_maxMessagesCount,
            DEFAULT_MAX_MESSAGES_COUNT
        )
        typedArray.recycle()
        return maxMessagesCount
    }
    private fun readScrollDelay(context: Context, attrs: AttributeSet?, defStyleAttr: Int, sRes: Int): Int {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ContentBlockCarouselView, defStyleAttr, sRes)
        val scrollDelay = typedArray.getInt(R.styleable.ContentBlockCarouselView_scrollDelay, DEFAULT_SCROLL_DELAY)
        typedArray.recycle()
        return scrollDelay
    }
    private fun init(
        context: Context,
        placeholderId: String,
        maxMessagesCount: Int,
        scrollDelay: Int
    ) {
        Logger.i(this, "InAppCbCarousel: Initializing Carousel for content block placeholder ID: $placeholderId")
        if (placeholderId == EMPTY_PLACEHOLDER_ID) {
            Logger.e(this, "InAppCbCarousel: Placeholder ID is required")
        }
        this.viewController = ContentBlockCarouselViewController(
            this,
            placeholderId,
            maxMessagesCount,
            scrollDelay
        )
        View.inflate(context, R.layout.inapp_content_block_carousel, this)
        this.viewPager = this.content_block_carousel_pager
        this.viewPager.adapter = viewController.contentBlockCarouselAdapter
        this.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                viewController.onPageScrollStateChanged(
                    state,
                    viewPager.currentItem
                )
            }
        })
        CustomTabsClient.bindCustomTabsService(
            context, CustomTabsClient.getPackageName(context, null), tabsConnection
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Logger.v(this, "InAppCbCarousel: View is attached to window")
        viewController.onViewAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        Logger.v(this, "InAppCbCarousel: View is detached from window")
        viewController.onViewDetachedFromWindow()
        super.onDetachedFromWindow()
    }

    fun reload() {
        Logger.i(this, "InAppCbCarousel: Reload requested programmatically")
        viewController.reload()
    }

    fun getShownContentBlock() = viewController.getShownContentBlock()

    fun getShownIndex() = viewController.getShownIndex()

    fun getShownCount() = viewController.getShownCount()
    internal fun prepareOffscreenPages(count: Int) {
        viewPager.offscreenPageLimit = count
    }

    internal fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        viewPager.setCurrentItem(item, smoothScroll)
    }

    internal fun recalculateHeightIfNeeded(onlyCurrentView: Boolean) {
        if (onlyCurrentView) {
            Logger.v(this, "InAppCbCarousel: Recalculating height for current view")
        } else {
            Logger.v(this, "InAppCbCarousel: Recalculating height for current view and views around it")
        }
        if (layoutParams == null || layoutParams.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
            Logger.v(this, "InAppCbCarousel: Auto-height is not set, skipping")
            return
        }
        val viewHolderScope = collectActivePlaceholderViews(onlyCurrentView)
        val planned = post {
            val highestValue = viewHolderScope.map {
                it.measure(
                    MeasureSpec.makeMeasureSpec(it.width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                )
                Logger.v(
                    this,
                    "InAppCbCarousel: Auto-height feature detects height: ${it.measuredHeight}"
                )
                it.measuredHeight
            }.maxOrNull()
            Logger.v(this, "InAppCbCarousel: Max height of content block is: $highestValue")
            highestValue?.let {
                if (viewPager.layoutParams.height != it) {
                    viewPager.layoutParams = viewPager.layoutParams.apply {
                        height = it
                    }
                }
            }
        }
        if (!planned) {
            Logger.w(this, "InAppCbCarousel: Auto-height feature disabled, view is exiting")
            return
        }
    }

    private fun collectActivePlaceholderViews(onlyCurrentView: Boolean): List<InAppContentBlockPlaceholderView> {
        val result = mutableListOf<InAppContentBlockPlaceholderView>()
        val innerRecyclerView = viewPager.getChildAt(0)
        if (innerRecyclerView !is RecyclerView) {
            Logger.w(this, "InAppCbCarousel: Auto-height feature disabled, inner view is not a RecyclerView")
            return result
        }
        // previous
        if (!onlyCurrentView) {
            (innerRecyclerView.findViewHolderForLayoutPosition(
                viewPager.currentItem - 1
            ) as? ContentBlockCarouselViewHolder)?.getContentBlockPlaceholderView()?.let {
                result.add(it)
            }
        }
        // current
        (innerRecyclerView.findViewHolderForLayoutPosition(
            viewPager.currentItem
        ) as? ContentBlockCarouselViewHolder)?.getContentBlockPlaceholderView()?.let {
            result.add(it)
        }
        // next
        if (!onlyCurrentView) {
            (innerRecyclerView.findViewHolderForLayoutPosition(
                viewPager.currentItem + 1
            ) as? ContentBlockCarouselViewHolder)?.getContentBlockPlaceholderView()?.let {
                result.add(it)
            }
        }
        return result
    }

    internal fun openInnerBrowser(url: String?) {
        if (url == null) {
            Logger.e(this, "InAppCbCarousel: Unable to open browser, url is null")
            return
        }
        if (tabsSession == null) {
            Logger.w(this, "InAppCbCarousel: Inner web browser lost session, app session may be closed")
        }
        CustomTabsIntent.Builder(tabsSession)
            .setShowTitle(true)
            .setInstantAppsEnabled(true)
            .setInitialActivityHeightPx(
                Resources.getSystem().displayMetrics.heightPixels,
                CustomTabsIntent.ACTIVITY_HEIGHT_ADJUSTABLE
            )
            .build()
            .launchUrl(context, Uri.parse(url))
    }
}

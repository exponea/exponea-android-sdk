package com.exponea.sdk.services.inappcontentblock

import android.content.Context
import androidx.viewpager2.widget.ViewPager2
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.InAppContentBlockManagerImpl
import com.exponea.sdk.models.ContentBlockCarouselCallback
import com.exponea.sdk.models.ContentBlockSelector
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.models.InAppContentBlockCallback
import com.exponea.sdk.services.ExponeaContextProvider
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.OnForegroundStateListener
import com.exponea.sdk.util.RepeatableJob
import com.exponea.sdk.util.ensureOnBackgroundThread
import com.exponea.sdk.util.runForInitializedSDK
import com.exponea.sdk.util.runOnMainThread
import com.exponea.sdk.view.ContentBlockCarouselView
import com.exponea.sdk.view.InAppContentBlockPlaceholderView
import java.util.concurrent.TimeUnit

internal class ContentBlockCarouselViewController(
    context: Context,
    private val carouselView: ContentBlockCarouselView,
    private val placeholderId: String = EMPTY_PLACEHOLDER_ID,
    private val maxMessagesCount: Int = DEFAULT_MAX_MESSAGES_COUNT,
    private val scrollDelay: Int = DEFAULT_SCROLL_DELAY
) : OnForegroundStateListener {

    companion object {
        internal const val EMPTY_PLACEHOLDER_ID = ""
        internal const val RELOAD_PROCESS_ID = "ContentBlockCarouselView_reload"
        internal const val DEFAULT_MAX_MESSAGES_COUNT = 0
        internal const val DEFAULT_SCROLL_DELAY = 3
    }

    private val showTrackedContentBlockIds = mutableSetOf<String>()
    internal var contentBlockSelector: ContentBlockSelector = ContentBlockSelector()
    private var defaultBehaviour = CarouselDefaultInAppContentCallback(context)
    internal var behaviourCallback: ContentBlockCarouselCallback? = null
    private val autoscrollJob: RepeatableJob = RepeatableJob(TimeUnit.SECONDS.toMillis(scrollDelay.toLong())) {
        scrollToNext()
    }
    private var selectedBlockIndex: Int = -1
    internal var contentBlockCarouselAdapter = ContentBlockCarouselAdapter(
        placeholderId = placeholderId,
        onPlaceholderCreated = {
            modifyPlaceholderBehaviour(it)
        }
    )

    fun onPageScrollStateChanged(state: Int, pagerIndex: Int) {
        // !!! `selectedBlockIndex` == `pagerIndex` - 1; always
        // see `contentBlockCarouselAdapter` implementation for `multiplyFirstAndLastItems`
        when (state) {
            ViewPager2.SCROLL_STATE_SETTLING -> {
                Logger.d(this, "InAppCbCarousel: Carousel is moving to next item")
                updateAutoHeight(false)
            }
            ViewPager2.SCROLL_STATE_DRAGGING -> {
                Logger.d(this, "InAppCbCarousel: Carousel is dragged by user")
                pauseAutoScroll()
                updateAutoHeight(false)
            }
            ViewPager2.SCROLL_STATE_IDLE -> {
                if (pagerIndex >= contentBlockCarouselAdapter.itemCount - 1) {
                    Logger.v(this, "InAppCbCarousel: Last page item reached, restarting loop from begin")
                    moveToIndex(0, false)
                    restartAutoScroll()
                } else if (pagerIndex <= 0) {
                    Logger.d(this, "InAppCbCarousel: First page item reached, restarting loop from end")
                    moveToIndex(contentBlockCarouselAdapter.getLoadedDataCount() - 1, false)
                    restartAutoScroll()
                } else if (selectedBlockIndex == pagerIndex - 1) {
                    Logger.d(this, "InAppCbCarousel: Page item not changed, resuming auto scroll")
                    resumeAutoScroll()
                } else {
                    Logger.d(this, "InAppCbCarousel: Page item changed, restarting auto scroll")
                    moveToIndex(pagerIndex - 1, false)
                    restartAutoScroll()
                }
                updateAutoHeight(true)
            }
        }
    }

    private fun updateAutoHeight(onlyCurrentView: Boolean) {
        carouselView.recalculateHeightIfNeeded(onlyCurrentView)
    }

    internal fun moveToIndex(itemIndex: Int, smoothScroll: Boolean) {
        carouselView.setCurrentItem(itemIndex + 1, smoothScroll)
        if (!smoothScroll) {
            // smoothScroll-ed update has to be updated within onPageScrollStateChanged
            selectedBlockIndex = itemIndex
            onSelectedBlockIndexChanged()
        }
    }

    fun reload() {
        if (placeholderId == EMPTY_PLACEHOLDER_ID) {
            Logger.e(this, "InAppCbCarousel: Placeholder ID is required, skipping data reload")
            return
        }
        stopAutoScroll()
        runForInitializedSDK(RELOAD_PROCESS_ID) {
            ensureOnBackgroundThread {
                val manager = Exponea.getComponent()?.inAppContentBlockManager
                if (manager == null) {
                    Logger.e(this, "InAppCbCarousel: Exponea SDK is not initialized properly")
                    return@ensureOnBackgroundThread
                }
                showTrackedContentBlockIds.clear()
                val allContentBlocks = manager.getAllInAppContentBlocksForPlaceholder(placeholderId)
                manager.loadContentIfNeededSync(allContentBlocks)
                val validContentBlocks = filterContentBlocks(allContentBlocks)
                val filteredContentBlocks = contentBlockSelector.filterContentBlocks(validContentBlocks)
                val sortedContentBlocks = contentBlockSelector.sortContentBlocks(filteredContentBlocks)
                val limitedContentBlocks = limitByMaxMessagesCount(sortedContentBlocks)
                contentBlockCarouselAdapter.updateData(limitedContentBlocks)
                runOnMainThread {
                    carouselView.prepareOffscreenPages(limitedContentBlocks.size + 2)
                    moveToIndex(0, false)
                    updateAutoHeight(true)
                }
                behaviourCallback?.onMessagesChanged(limitedContentBlocks.size, limitedContentBlocks)
                restartAutoScroll()
            }
        }
    }

    private fun filterContentBlocks(source: List<InAppContentBlock>): List<InAppContentBlock> {
        val manager = Exponea.getComponent()?.inAppContentBlockManager
        if (manager == null) {
            Logger.e(this, "InAppCbCarousel: Exponea SDK is not initialized, unable to filter content blocks")
            return source
        }
        val filteredContentBlocks = source.filter { each -> manager.passesFilters(each) }
        val validFilteredContentBlocks = filteredContentBlocks
            .filter { each -> isStatusValid(each) }
            .filter { each -> isContentSupportedToShow(each) }
        return validFilteredContentBlocks
    }

    fun onViewAttachedToWindow() {
        ExponeaContextProvider.registerForegroundStateListener(this)
        reload()
    }

    fun onViewDetachedFromWindow() {
        ExponeaContextProvider.removeForegroundStateListener(this)
        stopAutoScroll()
    }

    fun getShownContentBlock(): InAppContentBlock? {
        return contentBlockCarouselAdapter.getItem(getShownIndex())
    }

    fun getShownIndex(): Int {
        return if (getShownCount() == 0) {
            return -1
        } else {
            selectedBlockIndex
        }
    }

    fun getShownCount(): Int {
        return contentBlockCarouselAdapter.getLoadedDataCount()
    }

    override fun onStateChanged(isForeground: Boolean) {
        Logger.v(this, "InAppCbCarousel: State changed to $isForeground")
        if (isForeground) {
            Logger.d(this, "InAppCbCarousel: Resuming auto scroll because foreground state established")
            resumeAutoScroll()
        } else {
            Logger.d(this, "InAppCbCarousel: Pausing auto scroll because background state established")
            pauseAutoScroll()
        }
    }

    private fun modifyPlaceholderBehaviour(placeholder: InAppContentBlockPlaceholderView) {
        val originalBehaviour = placeholder.behaviourCallback
        placeholder.setOnContentReadyListener {
            updateAutoHeight(true)
        }
        placeholder.behaviourCallback = object : InAppContentBlockCallback {
            override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
                // 'show' event is handled by 'onSelectedBlockIndexChanged'
            }
            override fun onNoMessageFound(placeholderId: String) {
                originalBehaviour.onNoMessageFound(placeholderId)
            }
            override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {
                originalBehaviour.onError(placeholderId, contentBlock, errorMessage)
            }
            override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) {
                originalBehaviour.onCloseClicked(placeholderId, contentBlock)
                if (shouldBeRemovedAfterAction(contentBlock)) {
                    removeFromData(contentBlock)
                    restartAutoScroll()
                }
            }
            override fun onActionClicked(
                placeholderId: String,
                contentBlock: InAppContentBlock,
                action: InAppContentBlockAction
            ) {
                Logger.d(
                    this,
                    "InAppCbCarousel: Tracking of InApp Content Block ${contentBlock.id} action ${action.name}"
                )
                Exponea.trackInAppContentBlockClick(placeholderId, action, contentBlock)
                invokeAction(action)
                if (shouldBeRemovedAfterAction(contentBlock)) {
                    removeFromData(contentBlock)
                    // will be paused BUT we need to restart delay time
                    restartAutoScroll()
                }
                onViewBecomeBackground()
            }
        }
    }

    private fun invokeAction(action: InAppContentBlockAction) {
        carouselView.openInnerBrowser(action.url)
    }

    private fun shouldBeRemovedAfterAction(contentBlock: InAppContentBlock): Boolean {
        val manager = Exponea.getComponent()?.inAppContentBlockManager
        if (manager == null) {
            Logger.e(
                this,
                "InAppCbCarousel: Exponea SDK is not initialized properly, unable to check removal by action"
            )
            return false
        }
        return !manager.passesFrequencyFilter(contentBlock)
    }

    private fun removeFromData(contentBlock: InAppContentBlock) {
        contentBlockCarouselAdapter.removeItem(contentBlock)
        val currentData = contentBlockCarouselAdapter.getLoadedData()
        var nextSelectedBlockIndex = selectedBlockIndex - 1
        if (nextSelectedBlockIndex < 0 && currentData.isNotEmpty()) {
            nextSelectedBlockIndex = 0
        }
        moveToIndex(nextSelectedBlockIndex, false)
        behaviourCallback?.onMessagesChanged(currentData.size, currentData)
    }

    private fun restartAutoScroll() {
        if (scrollDelay < 0) {
            stopAutoScroll()
            return
        }
        autoscrollJob.restart()
    }

    private fun resumeAutoScroll() {
        autoscrollJob.resume()
    }

    private fun stopAutoScroll() {
        autoscrollJob.stop("InAppCbCarousel: Auto scroll stopped")
    }

    private fun pauseAutoScroll() {
        autoscrollJob.pause()
    }

    private fun limitByMaxMessagesCount(source: List<InAppContentBlock>): List<InAppContentBlock> {
        return if (maxMessagesCount <= 0) {
            source
        } else {
            source.take(maxMessagesCount)
        }
    }

    private fun onSelectedBlockIndexChanged() {
        val shownContentBlock = getShownContentBlock() ?: return
        if (showTrackedContentBlockIds.add(shownContentBlock.id)) {
            // will track 'shown' event and manages DisplayStatus
            defaultBehaviour.onMessageShown(placeholderId, shownContentBlock)
        } else {
            Logger.v(this, "InAppCbCarousel: Content block with ID ${shownContentBlock.id} already tracked as shown")
        }
        behaviourCallback?.onMessageShown(
            placeholderId,
            shownContentBlock,
            getShownIndex(),
            getShownCount()
        )
    }

    private fun scrollToNext() {
        val nextIndex = selectedBlockIndex + 1
        Logger.d(this, "InAppCbCarousel: Scroll to next requested ($nextIndex)")
        moveToIndex(nextIndex, true)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    private fun isContentSupportedToShow(contentBlock: InAppContentBlock): Boolean {
        if (InAppContentBlockManagerImpl.SUPPORTED_CONTENT_BLOCK_TYPES_TO_SHOW.contains(contentBlock.contentType)) {
            return true
        }
        Logger.i(this, "InAppCbCarousel: Block ${contentBlock.id} content is unsupported to show")
        return false
    }

    @Suppress("MemberVisibilityCanBePrivate")
    private fun isStatusValid(contentBlock: InAppContentBlock): Boolean {
        if (contentBlock.isStatusValid()) {
            return true
        }
        Logger.i(this, """
            InAppCbCarousel: Block ${contentBlock.id} filtered out because of status ${contentBlock.status}
            """.trimIndent()
        )
        return false
    }

    fun onViewBecomeForeground() {
        onStateChanged(true)
    }

    fun onViewBecomeBackground() {
        onStateChanged(false)
    }
}

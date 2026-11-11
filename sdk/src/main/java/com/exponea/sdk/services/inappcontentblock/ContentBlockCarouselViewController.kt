package com.exponea.sdk.services.inappcontentblock

import androidx.viewpager2.widget.ViewPager2
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.InAppContentBlockManager
import com.exponea.sdk.manager.InAppContentBlockManagerImpl
import com.exponea.sdk.models.ContentBlockCarouselCallback
import com.exponea.sdk.models.ContentBlockSelector
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.models.InAppContentBlockActionType
import com.exponea.sdk.models.InAppContentBlockCallback
import com.exponea.sdk.services.ExponeaContextProvider
import com.exponea.sdk.services.OnIntegrationStoppedCallback
import com.exponea.sdk.telemetry.model.TelemetryEvent
import com.exponea.sdk.util.ExponeaGson
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.OnForegroundStateListener
import com.exponea.sdk.util.RepeatableJob
import com.exponea.sdk.util.UrlOpener
import com.exponea.sdk.util.deepCopy
import com.exponea.sdk.util.ensureOnBackgroundThread
import com.exponea.sdk.util.ensureOnMainThread
import com.exponea.sdk.util.logOnException
import com.exponea.sdk.util.logOnExceptionWithResult
import com.exponea.sdk.util.returnOnException
import com.exponea.sdk.util.runForInitializedSDK
import com.exponea.sdk.util.runOnBackgroundThread
import com.exponea.sdk.util.runOnMainThread
import com.exponea.sdk.view.ContentBlockCarouselView
import com.exponea.sdk.view.InAppContentBlockPlaceholderView
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal class ContentBlockCarouselViewController(
    private val carouselView: ContentBlockCarouselView,
    private val placeholderId: String = EMPTY_PLACEHOLDER_ID,
    private val maxMessagesCount: Int = DEFAULT_MAX_MESSAGES_COUNT,
    private val scrollDelay: Int = DEFAULT_SCROLL_DELAY
) : OnForegroundStateListener, OnIntegrationStoppedCallback {

    companion object {
        internal const val EMPTY_PLACEHOLDER_ID = ""
        internal const val RELOAD_PROCESS_ID = "ContentBlockCarouselView_reload"
        internal const val DEFAULT_MAX_MESSAGES_COUNT = 0
        internal const val DEFAULT_SCROLL_DELAY = 3
        internal const val BOUNDARY_FLICKER_PRELOAD_PAGES = 2
    }

    private val showTrackedContentBlockIds = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    internal var contentBlockSelector: ContentBlockSelector = ContentBlockSelector()
    internal var behaviourCallback: ContentBlockCarouselCallback? = null
    private val autoscrollJob: RepeatableJob = RepeatableJob(TimeUnit.SECONDS.toMillis(scrollDelay.toLong())) {
        scrollToNext()
    }
    private var autoHeightUpdatePending = false
    private var autoHeightUpdateOnlyCurrentView = true
    @Volatile private var selectedBlockIndex: Int = -1
    // Reloads run concurrently; only the latest one is allowed to update shown data
    private val reloadGeneration = AtomicInteger(0)
    // Forced reload superseded by a newer one must not lose its forceRefresh
    private val pendingForceRefresh = AtomicBoolean(false)
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
        ensureOnMainThread {
            autoHeightUpdateOnlyCurrentView = autoHeightUpdateOnlyCurrentView && onlyCurrentView
            if (autoHeightUpdatePending) {
                return@ensureOnMainThread
            }
            autoHeightUpdatePending = true
            val planned = carouselView.post {
                val pendingOnlyCurrentView = autoHeightUpdateOnlyCurrentView
                autoHeightUpdateOnlyCurrentView = true
                autoHeightUpdatePending = false
                carouselView.recalculateHeightIfNeeded(pendingOnlyCurrentView)
            }
            if (!planned) {
                autoHeightUpdateOnlyCurrentView = true
                autoHeightUpdatePending = false
                Logger.w(this, "InAppCbCarousel: Auto-height update disabled, view is exiting")
            }
        }
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
        reload(forceRefresh = true)
    }

    internal fun reload(forceRefresh: Boolean) {
        if (placeholderId == EMPTY_PLACEHOLDER_ID) {
            Logger.e(this, "InAppCbCarousel: Placeholder ID is required, skipping data reload")
            return
        }
        if (Exponea.isStopped) {
            Logger.e(this, "In-app content blocks UI is unavailable, SDK is stopping")
            return
        }
        stopAutoScroll()
        if (forceRefresh) {
            // set before increment, so any newer reload (possibly from another thread) sees it
            pendingForceRefresh.set(true)
        }
        val generation = reloadGeneration.incrementAndGet()
        runForInitializedSDK(RELOAD_PROCESS_ID) {
            ensureOnBackgroundThread {
                val manager = Exponea.getComponent()?.inAppContentBlockManager
                if (manager == null) {
                    Logger.e(this, "InAppCbCarousel: Exponea SDK is not initialized properly")
                    return@ensureOnBackgroundThread
                }
                if (generation != reloadGeneration.get()) {
                    // newer reload has been requested meanwhile, skip fetching of content
                    Logger.d(this, "InAppCbCarousel: Reload is outdated, skipping")
                    return@ensureOnBackgroundThread
                }
                // forced reload superseded by this one has to be fulfilled here
                val effectiveForceRefresh = forceRefresh || pendingForceRefresh.get()
                val allContentBlocks = manager.getAllInAppContentBlocksForPlaceholder(placeholderId)
                manager.loadContentIfNeededSync(allContentBlocks, forceRefresh = effectiveForceRefresh)
                if (effectiveForceRefresh && generation == reloadGeneration.get()) {
                    // forced fetch is done; newer reload keeps the flag for itself
                    pendingForceRefresh.set(false)
                }
                val validContentBlocks = filterContentBlocks(allContentBlocks, manager)
                val filteredContentBlocks = runCatching {
                    contentBlockSelector.filterContentBlocks(validContentBlocks)
                }.logOnExceptionWithResult().returnOnException { validContentBlocks }
                val sortedContentBlocks = runCatching {
                    contentBlockSelector.sortContentBlocks(filteredContentBlocks)
                }.logOnExceptionWithResult().returnOnException { filteredContentBlocks }
                // Snapshot of content blocks; manager updates its instances by any later fetch
                val limitedContentBlocks = limitByMaxMessagesCount(sortedContentBlocks).map { contentBlock ->
                    runCatching {
                        contentBlock.deepCopy()
                    }.logOnExceptionWithResult().returnOnException { contentBlock }
                }
                // Keys are computed here to keep hashing of (possibly large) HTML off the main thread
                val limitedContentBlockKeys = limitedContentBlocks.map { ContentBlockItemKey.of(it) }
                limitedContentBlocks.forEach {
                    InAppContentBlockTiming.log(
                        contentBlockId = it.id,
                        source = InAppContentBlockTiming.RequestSource.CAROUSEL_RELOADED,
                        contentBlockName = it.name,
                        properties = mapOf("forceRefresh" to effectiveForceRefresh)
                    )
                }
                runOnMainThread {
                    if (generation != reloadGeneration.get()) {
                        Logger.d(this, "InAppCbCarousel: Reload result is outdated, skipping")
                        return@runOnMainThread
                    }
                    // cleared only for applied reload; outdated one must not reset tracking of shown data
                    showTrackedContentBlockIds.clear()
                    contentBlockCarouselAdapter.updateData(limitedContentBlocks, limitedContentBlockKeys)
                    carouselView.prepareOffscreenPages(
                        if (limitedContentBlocks.size > 1) {
                            BOUNDARY_FLICKER_PRELOAD_PAGES
                        } else {
                            ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
                        }
                    )
                    moveToIndex(0, false)
                    updateAutoHeight(true)
                    restartAutoScroll()
                    // Callbacks are invoked on background thread, as integrators may expect
                    runOnBackgroundThread {
                        if (generation != reloadGeneration.get()) {
                            Logger.d(this, "InAppCbCarousel: Reload result is outdated, skipping callbacks")
                            return@runOnBackgroundThread
                        }
                        notifyMessagesChanged(limitedContentBlocks, notifyNoMessage = true)
                    }
                }
            }
        }
    }

    private fun filterContentBlocks(
        source: List<InAppContentBlock>,
        manager: InAppContentBlockManager
    ): List<InAppContentBlock> {
        if (manager is InAppContentBlockManagerImpl) {
            return manager.filterContentBlocksForDisplay(source)
        }
        val filteredContentBlocks = source.filter { each -> manager.passesFilters(each) }
        val validFilteredContentBlocks = filteredContentBlocks
            .filter { each -> isStatusValid(each) }
            .filter { each -> isContentSupportedToShow(each) }
        return validFilteredContentBlocks
    }

    fun onViewAttachedToWindow() {
        if (Exponea.isStopped) {
            Logger.e(this, "In-app content blocks UI is unavailable, SDK is stopping")
            return
        }
        ExponeaContextProvider.registerForegroundStateListener(this)
        reload(forceRefresh = false)
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
        if (Exponea.isStopped) {
            Logger.e(this, "In-app content blocks UI is unavailable, SDK is stopping")
            return
        }
        if (isForeground) {
            Logger.d(this, "InAppCbCarousel: Resuming auto scroll because foreground state established")
            resumeAutoScroll()
        } else {
            Logger.d(this, "InAppCbCarousel: Pausing auto scroll because background state established")
            pauseAutoScroll()
        }
    }

    internal fun modifyPlaceholderBehaviour(placeholder: InAppContentBlockPlaceholderView) {
        placeholder.controller.reloadAfterAction = false
        placeholder.setOnContentReadyListener {
            updateAutoHeight(true)
        }
        placeholder.setOnHeightUpdateListener {
            updateAutoHeight(true)
        }
        placeholder.behaviourCallback = object : InAppContentBlockCallback {
            override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
                // 'show' event is handled by 'onSelectedBlockIndexChanged'
            }
            override fun onNoMessageFound(placeholderId: String) {
                Logger.i(this, "InAppCbCarousel: InApp Content Block has no content for $placeholderId")
            }
            override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {
                behaviourCallback?.let {
                    runCatching {
                        it.onError(
                            placeholderId,
                            contentBlock,
                            errorMessage
                        )
                    }.logOnException()
                }
                if (contentBlock == null) {
                    Logger.e(this, "InApp Content Block is empty!!! Nothing to track")
                    return
                }
                Logger.d(this, "InAppCbCarousel: Tracking of InApp Content Block ${contentBlock.id} error")
                Exponea.trackInAppContentBlockError(placeholderId, contentBlock, errorMessage)
            }
            override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) {
                if (behaviourCallback?.trackActions != false) {
                    Logger.d(this, "InAppCbCarousel: Tracking of InApp Content Block ${contentBlock.id} close")
                    Exponea.trackInAppContentBlockClose(placeholderId, contentBlock)
                }
                behaviourCallback?.let {
                    runCatching {
                        it.onCloseClicked(
                            placeholderId,
                            contentBlock
                        )
                    }.logOnException()
                }
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
                if (behaviourCallback?.trackActions != false) {
                    Logger.d(
                        this,
                        "InAppCbCarousel: Tracking of InApp Content Block ${contentBlock.id} action ${action.name}"
                    )
                    Exponea.trackInAppContentBlockClick(placeholderId, action, contentBlock)
                }
                if (behaviourCallback?.overrideDefaultBehavior != true) {
                    invokeAction(action)
                }
                behaviourCallback?.let {
                    kotlin.runCatching {
                        it.onActionClicked(
                            placeholderId,
                            contentBlock,
                            action
                        )
                    }.logOnException()
                }
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
        when (action.type) {
            InAppContentBlockActionType.CLOSE -> return
            InAppContentBlockActionType.DEEPLINK -> UrlOpener.openUrlInApp(
                context = carouselView.context,
                url = action.url
            )
            InAppContentBlockActionType.BROWSER -> carouselView.openInnerBrowser(action.url)
        }
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

    private fun removeFromData(contentBlock: InAppContentBlock) = ensureOnMainThread {
        contentBlockCarouselAdapter.removeItem(contentBlock)
        val currentData = contentBlockCarouselAdapter.getLoadedData()
        val nextSelectedBlockIndex = if (currentData.isEmpty()) {
            -1
        } else {
            (selectedBlockIndex - 1).coerceIn(0, currentData.size - 1)
        }
        moveToIndex(nextSelectedBlockIndex, false)
        notifyMessagesChanged(currentData, notifyNoMessage = false)
    }

    private fun notifyMessagesChanged(contentBlocks: List<InAppContentBlock>, notifyNoMessage: Boolean) {
        behaviourCallback?.let {
            runCatching {
                it.onMessagesChanged(contentBlocks.size, contentBlocks)
            }.logOnException()
        }
        if (notifyNoMessage && contentBlocks.isEmpty()) {
            behaviourCallback?.let {
                runCatching {
                    it.onNoMessageFound(placeholderId)
                }.logOnException()
            }
        }
    }

    private fun restartAutoScroll() {
        ensureOnMainThread {
            if (!canAutoScroll()) {
                stopAutoScroll()
                return@ensureOnMainThread
            }
            autoscrollJob.restart()
        }
    }

    private fun resumeAutoScroll() {
        ensureOnMainThread {
            if (!canAutoScroll()) {
                stopAutoScroll()
                return@ensureOnMainThread
            }
            autoscrollJob.resume()
        }
    }

    private fun canAutoScroll() = scrollDelay >= 0 && getShownCount() > 1

    private fun stopAutoScroll() {
        ensureOnMainThread {
            autoscrollJob.stop("InAppCbCarousel: Auto scroll stopped")
        }
    }

    private fun pauseAutoScroll() {
        ensureOnMainThread {
            autoscrollJob.pause()
        }
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
            Logger.d(this, "InAppCbCarousel: Tracking of InApp Content Block ${shownContentBlock.id} show")
            Exponea.trackInAppContentBlockShown(placeholderId, shownContentBlock)
            Exponea.telemetry?.reportEvent(TelemetryEvent.CONTENT_BLOCK_SHOWN, hashMapOf(
                "type" to "static",
                "messageId" to shownContentBlock.id,
                "placeholders" to ExponeaGson.instance.toJson(shownContentBlock.placeholders)
            ))
        } else {
            Logger.v(this, "InAppCbCarousel: Content block with ID ${shownContentBlock.id} already tracked as shown")
        }
        behaviourCallback?.let {
            runCatching {
                it.onMessageShown(
                    placeholderId,
                    shownContentBlock,
                    getShownIndex(),
                    getShownCount()
                )
            }.logOnException()
        }
    }

    private fun scrollToNext() {
        if (getShownCount() <= 1) {
            Logger.v(this, "InAppCbCarousel: Nothing to scroll to, skipping")
            return
        }
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

    fun onHeightChanged(newHeight: Int) {
        behaviourCallback?.let {
            runCatching {
                it.onHeightUpdate(placeholderId, newHeight)
            }.logOnException()
        }
    }

    override fun onIntegrationStopped() {
        reloadGeneration.incrementAndGet()
        pendingForceRefresh.set(false)
        stopAutoScroll()
        showTrackedContentBlockIds.clear()
        ensureOnMainThread {
            contentBlockCarouselAdapter.updateData(emptyList())
        }
        behaviourCallback?.let {
            runCatching {
                it.onMessagesChanged(0, emptyList())
            }.logOnException()
        }
        behaviourCallback?.let {
            runCatching {
                it.onNoMessageFound(placeholderId)
            }.logOnException()
        }
    }
}

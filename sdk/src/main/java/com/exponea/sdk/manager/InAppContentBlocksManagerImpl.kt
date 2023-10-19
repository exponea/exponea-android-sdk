package com.exponea.sdk.manager

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.exponea.sdk.manager.TrackingConsentManager.MODE.CONSIDER_CONSENT
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.models.InAppContentBlockActionType
import com.exponea.sdk.models.InAppContentBlockPersonalizedData
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
import com.exponea.sdk.models.InAppContentBlockStatus.OK
import com.exponea.sdk.models.InAppContentBlockType
import com.exponea.sdk.models.InAppContentBlockType.NOT_DEFINED
import com.exponea.sdk.repository.BitmapCache
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.HtmlNormalizedCache
import com.exponea.sdk.repository.InAppContentBlockDisplayStateRepository
import com.exponea.sdk.repository.SimpleFileCache
import com.exponea.sdk.services.ExponeaProjectFactory
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockActionDispatcher
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockComparator
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockDataLoader
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockViewController
import com.exponea.sdk.util.ExponeaGson
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.ThreadSafeAccess
import com.exponea.sdk.util.currentTimeSeconds
import com.exponea.sdk.util.ensureOnBackgroundThread
import com.exponea.sdk.util.fromJson
import com.exponea.sdk.util.runOnBackgroundThread
import com.exponea.sdk.view.InAppContentBlockPlaceholderView
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

internal class InAppContentBlocksManagerImpl(
    private val displayStateRepository: InAppContentBlockDisplayStateRepository,
    private val fetchManager: FetchManager,
    private val projectFactory: ExponeaProjectFactory,
    private val customerIdsRepository: CustomerIdsRepository,
    private val trackingConsentManager: TrackingConsentManager,
    private val imageCache: BitmapCache,
    private val htmlCache: HtmlNormalizedCache,
    private val fontCache: SimpleFileCache
) : InAppContentBlockManager, InAppContentBlockActionDispatcher, InAppContentBlockDataLoader {

    private val SUPPORTED_CONTENT_BLOCK_TYPES_TO_SHOW = listOf(
        InAppContentBlockType.HTML
    )
    private val SUPPORTED_CONTENT_BLOCK_TYPES_TO_DOWNLOAD = SUPPORTED_CONTENT_BLOCK_TYPES_TO_SHOW + NOT_DEFINED
    private val CONTENT_LOAD_TIMEOUT: Long = 10

    private var sessionStartDate = Date()
    internal var contentBlocksData: List<InAppContentBlock> = emptyList()

    internal val dataAccess = ThreadSafeAccess()

    override fun onEventCreated(event: Event, type: EventType) {
        when (type) {
            EventType.SESSION_START -> {
                val eventTimestampInMillis = (event.timestamp ?: currentTimeSeconds()) * 1000
                sessionStartDate = Date(eventTimestampInMillis.toLong())
            }
            EventType.TRACK_CUSTOMER -> {
                runOnBackgroundThread {
                    Logger.i(this, "CustomerIDs are updated, clearing InApp content personalized blocks")
                    clearPersonalizationAssignments()
                    reassignCustomerIds()
                }
            }
            else -> {
                // nothing to trigger
            }
        }
    }

    override fun getPlaceholderView(
        placeholderId: String,
        context: Context,
        config: InAppContentBlockPlaceholderConfiguration
    ): InAppContentBlockPlaceholderView {
        val viewController = InAppContentBlockViewController(
            placeholderId,
            config,
            imageCache,
            fontCache,
            htmlCache,
            this,
            this
        )
        return viewController.getPlaceholderView(context)
    }

    override fun clearAll() = runThreadSafelyInBackground {
        contentBlocksData.forEach { contentBlock ->
            htmlCache.remove(contentBlock.id)
        }
        contentBlocksData = emptyList()
        displayStateRepository.clear()
        Logger.d(this, "InApp Content Blocks was cleared completely")
    }

    private fun runThreadSafelyInBackground(action: () -> Unit) {
        ensureOnBackgroundThread {
            dataAccess.waitForAccess(action)
        }
    }

    private fun <T> runThreadSafelyWithResult(action: () -> T): T? {
        return dataAccess.waitForAccessWithResult(action).getOrNull()
    }

    private fun clearPersonalizationAssignments() = runThreadSafelyInBackground {
        contentBlocksData.forEach {
            it.personalizedData = null
        }
        displayStateRepository.clear()
        Logger.d(this, "InApp Content Blocks was cleared from personal assignments")
    }

    private fun reassignCustomerIds() = runThreadSafelyInBackground {
        contentBlocksData.forEach {
            it.customerIds = customerIdsRepository.get().toHashMap()
        }
    }

    private fun updateContentForLocalContentBlocks(source: List<InAppContentBlock>) = runThreadSafelyInBackground {
        val dataMap = source.groupBy { it.id }
        contentBlocksData.forEach { targetContentBlock ->
            dataMap.get(targetContentBlock.id)?.firstOrNull()?.let { sourceContentBlock ->
                targetContentBlock.personalizedData = sourceContentBlock.personalizedData
            }
        }
    }

    /**
     * Loads missing or obsolete content for block.
     */
    private fun loadContentIfNeededSync(contentBlocks: List<InAppContentBlock>) = runThreadSafelyInBackground {
        val blockIds = contentBlocks
            .filter { !it.hasFreshContent() }
            .map { it.id }
        if (blockIds.isEmpty()) {
            return@runThreadSafelyInBackground
        }
        Logger.i(this, "Loading content for InApp Content Blocks ${blockIds.joinToString()}")
        val semaphore = CountDownLatch(1)
        prefetchContentForBlocks(
            blockIds,
            onSuccess = { contentData ->
                val dataMap = contentData.groupBy { it.blockId }
                contentBlocks.forEach { contentBlock ->
                    dataMap.get(contentBlock.id)?.firstOrNull()?.let {
                        contentBlock.personalizedData = it
                    }
                }
                semaphore.countDown()
            },
            onFailure = {
                // dont do anything, showing will fail
                semaphore.countDown()
            }
        )
        if (!semaphore.await(CONTENT_LOAD_TIMEOUT, SECONDS)) {
            Logger.w(this, "Loading content for InApp Content Blocks timeout")
        }
    }

    private fun prefetchContentForBlocks(
        contentBlockIds: List<String>,
        onSuccess: (List<InAppContentBlockPersonalizedData>) -> Unit,
        onFailure: (FetchError) -> Unit
    ) {
        val customerIds = customerIdsRepository.get()
        fetchManager.fetchPersonalizedContentBlocks(
            exponeaProject = projectFactory.mutualExponeaProject,
            customerIds = customerIds,
            contentBlockIds = contentBlockIds,
            onSuccess = { result ->
                Logger.i(this, "Content for InApp Content Blocks $contentBlockIds loaded")
                val data = result.results ?: emptyList()
                data.forEach {
                    it.loadedAt = Date()
                }
                onSuccess(data)
            },
            onFailure = {
                Logger.e(this, "InAppContentBlock data load failed. ${it.results.message}")
                onFailure(it.results)
            }
        )
    }

    private fun pickInAppContentBlock(placeholderId: String): InAppContentBlock? {
        Logger.d(this, "Picking InAppContentBlock for placeholder $placeholderId starts")
        val allContentBlocks = getInAppContentBlocksForPlaceholder(placeholderId)
        Logger.d(this,
            """Got ${allContentBlocks.size} content blocks:
                ${ExponeaGson.instance.toJson(allContentBlocks)}
                """.trimIndent())
        val filteredContentBlocks = allContentBlocks.filter { each -> passesFilters(each) }
        loadContentIfNeededSync(filteredContentBlocks)
        updateContentForLocalContentBlocks(filteredContentBlocks)
        val validFilteredContentBlocks = filteredContentBlocks
            .filter { each -> isStatusValid(each) }
            .filter { each -> isContentSupported(each) }
        val sortedContentBlocks = validFilteredContentBlocks.sortedWith(InAppContentBlockComparator.INSTANCE)
        Logger.i(this, "Got ${sortedContentBlocks.size} content blocks for placeholder $placeholderId")
        Logger.d(this, """
            Placeholder $placeholderId can show content blocks:
            ${ExponeaGson.instance.toJson(sortedContentBlocks)}
            """.trimIndent())
        // create unmutable copy due to updating of content blocks data while other loadings
        return deepCopy(sortedContentBlocks.firstOrNull())
    }

    private fun isStatusValid(contentBlock: InAppContentBlock): Boolean {
        if (contentBlock.status == OK) {
            return true
        }
        Logger.i(this, """
            InApp Content Block ${contentBlock.id} filtered out because of status ${contentBlock.status}
            """.trimIndent()
        )
        return false
    }

    private fun isContentSupported(contentBlock: InAppContentBlock): Boolean {
        if (SUPPORTED_CONTENT_BLOCK_TYPES_TO_SHOW.contains(contentBlock.contentType)) {
            return true
        }
        Logger.i(this, "InApp Content Block ${contentBlock.id} is invalid to be shown")
        return false
    }

    internal fun deepCopy(source: InAppContentBlock?): InAppContentBlock? {
        if (source == null) {
            return null
        }
        val sourceJson = ExponeaGson.instance.toJson(source)
        val target: InAppContentBlock = ExponeaGson.instance.fromJson(sourceJson)
        source.personalizedData?.let { sourcePersonData ->
            // if source has data, target must too
            target.personalizedData!!.loadedAt = Date(sourcePersonData.loadedAt!!.time)
        }
        return target
    }

    private fun passesFilters(contentBlock: InAppContentBlock): Boolean {
        Logger.i(this, "Validating filters for Content Block ${contentBlock.id}")
        val dateFilterPass = contentBlock.applyDateFilter(System.currentTimeMillis() / 1000)
        Logger.i(this, "InApp Content Block ${contentBlock.id} date-filter passed: $dateFilterPass")
        if (!dateFilterPass) {
            return false
        }
        val frequencyFilter = contentBlock.applyFrequencyFilter(
            displayStateRepository.get(contentBlock),
            sessionStartDate
        )
        Logger.i(this, "InApp Content Block ${contentBlock.id} frequency-filter passed: $frequencyFilter")
        return frequencyFilter
    }

    private fun getInAppContentBlocksForPlaceholder(
        placeholderId: String
    ): List<InAppContentBlock> = runThreadSafelyWithResult {
        val contentBlocksForPlaceholder = contentBlocksData.filter { block ->
            block.placeholders.contains(placeholderId)
        }
        Logger.i(this,
            """${contentBlocksForPlaceholder.size} InApp Content Blocks found for placeholder $placeholderId
            """.trimIndent()
        )
        Logger.d(this,
            """Found Content Blocks for placeholder $placeholderId:
            ${ExponeaGson.instance.toJson(contentBlocksForPlaceholder)}
            """.trimIndent()
        )
        return@runThreadSafelyWithResult contentBlocksForPlaceholder
    } ?: listOf()

    override fun loadInAppContentBlockPlaceholders() {
        Logger.d(this, "Loading of InApp Content Block placeholders requested")
        ensureOnBackgroundThread {
            dataAccess.waitForAccessWithDone { done ->
                Logger.d(this, "Loading of InApp Content Block placeholders")
                val customerIds = customerIdsRepository.get()
                val exponeaProject = projectFactory.mutualExponeaProject
                fetchManager.fetchStaticInAppContentBlocks(
                    exponeaProject = exponeaProject,
                    onSuccess = { result ->
                        Logger.i(this, "InApp Content Block placeholders preloaded successfully")
                        val inAppContentBlocks = result.results ?: emptyList()
                        val supportedContentBlocks = inAppContentBlocks.filter {
                            SUPPORTED_CONTENT_BLOCK_TYPES_TO_DOWNLOAD.contains(it.contentType)
                        }
                        supportedContentBlocks.forEach {
                            it.customerIds = customerIds.toHashMap()
                        }
                        forceContentByPlaceholders(
                            supportedContentBlocks,
                            exponeaProject.inAppContentBlockPlaceholdersAutoLoad
                        )
                        contentBlocksData = supportedContentBlocks
                        done()
                    },
                    onFailure = {
                        Logger.e(this, "InApp Content Block placeholders failed. ${it.results.message}")
                        done()
                    }
                )
            }
        }
    }

    private fun forceContentByPlaceholders(target: List<InAppContentBlock>, autoLoadPlaceholders: List<String>) {
        Logger.d(this, "InApp Content Blocks prefetch starts for placeholders: $autoLoadPlaceholders")
        val contentBlocksToLoad = target
            .filter { it.placeholders.intersect(autoLoadPlaceholders).isNotEmpty() }
        if (contentBlocksToLoad.isEmpty()) {
            Logger.i(this, "No InApp Content Block going to be prefetched")
            return
        }
        loadContentIfNeededSync(contentBlocksToLoad)
    }

    override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {
        Logger.w(this, "InApp Content Block ${contentBlock?.id ?: "no_ID"} has error $errorMessage")
        trackError(placeholderId, contentBlock, errorMessage)
    }

    override fun onClose(placeholderId: String, contentBlock: InAppContentBlock) {
        Logger.i(this, "InApp Content Block ${contentBlock.id} was closed")
        displayStateRepository.setInteracted(contentBlock, Date())
        trackClose(placeholderId, contentBlock)
    }

    override fun onAction(
        placeholderId: String,
        contentBlock: InAppContentBlock,
        action: InAppContentBlockAction,
        context: Context
    ) {
        Logger.i(this, "InApp Content Block ${contentBlock.id} requested action ${action.name}")
        displayStateRepository.setInteracted(contentBlock, Date())
        trackAction(placeholderId, action)
        invokeAction(action, context)
    }

    override fun onNoContent(placeholderId: String, contentBlock: InAppContentBlock?) {
        Logger.i(this, "InApp Content Block ${contentBlock?.id ?: "no_ID"} has no content")
        contentBlock?.let {
            // possibility of AB testing
            displayStateRepository.setDisplayed(it, Date())
            trackShown(placeholderId, it)
        }
    }

    override fun onShown(placeholderId: String, contentBlock: InAppContentBlock) {
        Logger.i(this, "InApp Content Block ${contentBlock.id} has been shown")
        displayStateRepository.setDisplayed(contentBlock, Date())
        trackShown(placeholderId, contentBlock)
    }

    private fun trackAction(placeholderId: String, action: InAppContentBlockAction) {
        Logger.d(this, "Tracking of InApp Content Block ${action.contentBlock.id} action ${action.name}")
        trackingConsentManager.trackInAppContentBlockClick(
            placeholderId,
            action.contentBlock,
            action.name,
            action.url,
            CONSIDER_CONSENT
        )
    }

    private fun trackClose(placeholderId: String, contentBlock: InAppContentBlock) {
        Logger.d(this, "Tracking of InApp Content Block ${contentBlock.id} close")
        trackingConsentManager.trackInAppContentBlockClose(placeholderId, contentBlock, CONSIDER_CONSENT)
    }

    private fun trackShown(placeholderId: String, contentBlock: InAppContentBlock) {
        Logger.d(this, "Tracking of InApp Content Block ${contentBlock.id} show")
        trackingConsentManager.trackInAppContentBlockShown(placeholderId, contentBlock, CONSIDER_CONSENT)
    }

    private fun trackError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {
        Logger.d(this, "Tracking of InApp Content Block ${contentBlock?.id ?: "no_ID"} error")
        if (contentBlock == null) {
            Logger.e(this, "InApp Content Block is empty!!! Nothing to track")
            return
        }
        trackingConsentManager.trackInAppContentBlockError(placeholderId, contentBlock, errorMessage, CONSIDER_CONSENT)
    }

    private fun invokeAction(action: InAppContentBlockAction, context: Context) {
        Logger.d(this, "Invoking InApp Content Block ${action.contentBlock.id} action '${action.name}'")
        val actionUrl = try {
            Uri.parse(action.url)
        } catch (e: Exception) {
            Logger.e(this, "InApp Content Block ${action.contentBlock.id} action ${action.url} is invalid", e)
            return
        }
        if (action.type == InAppContentBlockActionType.DEEPLINK || action.type == InAppContentBlockActionType.BROWSER) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        data = actionUrl
                    }
                )
            } catch (e: ActivityNotFoundException) {
                Logger.e(this, "InApp Content Block action failed", e)
            }
        }
    }

    override fun loadContent(placeholderId: String): InAppContentBlock? {
        val contentBlock = pickInAppContentBlock(placeholderId)
        if (contentBlock == null) {
            Logger.i(this, "No InApp Content Block found for placeholder ${placeholderId}\"")
        } else {
            Logger.i(this, "InApp Content Block ${contentBlock.id} for placeholder ${placeholderId}\"")
            loadContentIfNeededSync(listOf(contentBlock))
            // update content in storage just in case
            updateContentForLocalContentBlocks(listOf(contentBlock))
        }
        return contentBlock
    }
}

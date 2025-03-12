package com.exponea.sdk.manager

import android.content.Context
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.models.InAppContentBlockPersonalizedData
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
import com.exponea.sdk.models.InAppContentBlockType
import com.exponea.sdk.models.InAppContentBlockType.NOT_DEFINED
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.repository.FontCache
import com.exponea.sdk.repository.HtmlNormalizedCache
import com.exponea.sdk.repository.InAppContentBlockDisplayStateRepository
import com.exponea.sdk.services.ExponeaProjectFactory
import com.exponea.sdk.services.inappcontentblock.DefaultInAppContentCallback
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockActionDispatcher
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockComparator
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockDataLoader
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockViewController
import com.exponea.sdk.util.ExponeaGson
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.ThreadSafeAccess
import com.exponea.sdk.util.currentTimeSeconds
import com.exponea.sdk.util.deepCopy
import com.exponea.sdk.util.ensureOnBackgroundThread
import com.exponea.sdk.view.InAppContentBlockPlaceholderView
import java.util.Date

internal class InAppContentBlockManagerImpl(
    private val displayStateRepository: InAppContentBlockDisplayStateRepository,
    private val fetchManager: FetchManager,
    private val projectFactory: ExponeaProjectFactory,
    private val customerIdsRepository: CustomerIdsRepository,
    private val imageCache: DrawableCache,
    private val htmlCache: HtmlNormalizedCache,
    private val fontCache: FontCache
) : InAppContentBlockManager, InAppContentBlockActionDispatcher, InAppContentBlockDataLoader {

    companion object {
        internal val SUPPORTED_CONTENT_BLOCK_TYPES_TO_SHOW = listOf(
            InAppContentBlockType.HTML
        )
    }

    private val SUPPORTED_CONTENT_BLOCK_TYPES_TO_DOWNLOAD = SUPPORTED_CONTENT_BLOCK_TYPES_TO_SHOW + NOT_DEFINED

    private var sessionStartDate = Date()
    internal var contentBlocksData: List<InAppContentBlock> = emptyList()

    internal val dataAccess = ThreadSafeAccess()

    override fun onEventCreated(event: Event, type: EventType) {
        when (type) {
            EventType.SESSION_START -> {
                Logger.d(this, "InAppCB: Event session_start occurs, storing time value")
                val eventTimestampInMillis = (event.timestamp ?: currentTimeSeconds()) * 1000
                sessionStartDate = Date(eventTimestampInMillis.toLong())
            }
            EventType.TRACK_CUSTOMER -> {
                ensureOnBackgroundThread {
                    Logger.i(this, "InAppCB: CustomerIDs are updated, clearing personalized content")
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
    ): InAppContentBlockPlaceholderView = getPlaceholderView(
        placeholderId,
        this,
        context,
        config
    )

    override fun getPlaceholderView(
        placeholderId: String,
        dataLoader: InAppContentBlockDataLoader,
        context: Context,
        config: InAppContentBlockPlaceholderConfiguration
    ): InAppContentBlockPlaceholderView {
        val controller = InAppContentBlockViewController(
            placeholderId,
            config,
            imageCache,
            fontCache,
            htmlCache,
            this,
            dataLoader,
            DefaultInAppContentCallback(context)
        )
        val view = InAppContentBlockPlaceholderView(
            context,
            controller
        )
        if (!config.defferedLoad) {
            controller.loadContent(false)
        }
        return view
    }

    override fun clearAll() = runThreadSafelyInBackground {
        contentBlocksData.forEach { contentBlock ->
            htmlCache.remove(contentBlock.id)
        }
        contentBlocksData = emptyList()
        displayStateRepository.clear()
        Logger.i(this, "InAppCB: All data and cache has been cleared completely")
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
        Logger.d(this, "InAppCB: All Content Blocks was cleared from personalized data")
    }

    private fun reassignCustomerIds() {
        val currentCustomerIds = customerIdsRepository.get().toHashMap()
        runThreadSafelyInBackground {
            contentBlocksData.forEach {
                it.customerIds = currentCustomerIds
            }
        }
    }

    private fun updateContentForLocalContentBlocks(source: List<InAppContentBlock>) {
        val dataMap = source.groupBy { it.id }
        Logger.d(this, "InAppCB: Request to update personalized content of ${dataMap.keys.joinToString()}")
        contentBlocksData.forEach { localContentBlock ->
            dataMap[localContentBlock.id]?.firstOrNull()?.let { newContentBlock ->
                localContentBlock.personalizedData = newContentBlock.personalizedData
            }
        }
    }

    /**
     * Loads missing or obsolete content for block.
     */
    override fun loadContentIfNeededSync(contentBlocks: List<InAppContentBlock>) {
        dataAccess.waitForAccessWithDone { done ->
            loadContentIfNeededAsync(contentBlocks) {
                done()
            }
        }
    }

    private fun loadContentIfNeededAsync(contentBlocks: List<InAppContentBlock>, done: () -> Unit) {
        val blockIdsToCheck = contentBlocks.map { it.id }
        val blockIdsToUpdate = contentBlocksData
            .filter { it.id in blockIdsToCheck && !it.hasFreshContent() }
            .map { it.id }
        if (blockIdsToUpdate.isEmpty()) {
            Logger.d(this, "InAppCB: All content of blocks are fresh, nothing to update")
            done()
            return
        }
        Logger.i(this, "InAppCB: Loading content for blocks: ${blockIdsToUpdate.joinToString()}")
        prefetchContentForBlocks(
            blockIdsToUpdate,
            onSuccess = { contentData ->
                val dataMap = contentData.groupBy { it.blockId }
                // update personalized data for requested 'contentBlocks'
                contentBlocks.forEach { contentBlock ->
                    dataMap[contentBlock.id]?.firstOrNull()?.let {
                        contentBlock.personalizedData = it
                    }
                }
                // update personalized data for local 'contentBlocksData'
                updateContentForLocalContentBlocks(contentBlocks)
                done()
            },
            onFailure = {
                // dont do anything, showing will fail
                done()
            }
        )
    }

    private fun prefetchContentForBlocks(
        contentBlockIds: List<String>,
        onSuccess: (List<InAppContentBlockPersonalizedData>) -> Unit,
        onFailure: (FetchError) -> Unit
    ) {
        val customerIds = customerIdsRepository.get()
        Logger.i(this, "InAppCB: Prefetching personalized content for current customer")
        val contentBlockIdsAsString = contentBlockIds.joinToString()
        fetchManager.fetchPersonalizedContentBlocks(
            exponeaProject = projectFactory.mutualExponeaProject,
            customerIds = customerIds,
            contentBlockIds = contentBlockIds,
            onSuccess = { result ->
                Logger.i(
                    this,
                    "InAppCB: Personalized content for blocks $contentBlockIdsAsString loaded"
                )
                val data = result.results ?: emptyList()
                data.forEach {
                    it.loadedAt = Date()
                }
                onSuccess(data)
            },
            onFailure = {
                val errorMessage = it.results.message
                Logger.e(
                    this,
                    "InAppCB: Personalized content for blocks $contentBlockIdsAsString failed: $errorMessage"
                )
                onFailure(it.results)
            }
        )
    }

    private fun pickInAppContentBlock(placeholderId: String): InAppContentBlock? {
        Logger.i(this, "InAppCB: Picking of InAppContentBlock for placeholder $placeholderId starts")
        val allContentBlocks = getAllInAppContentBlocksForPlaceholder(placeholderId)
        val filteredContentBlocks = allContentBlocks.filter { each -> passesFilters(each) }
        loadContentIfNeededSync(filteredContentBlocks)
        val validFilteredContentBlocks = filteredContentBlocks
            .filter { each -> isStatusValid(each) }
            .filter { each -> isContentSupportedToShow(each) }
        val sortedContentBlocks = validFilteredContentBlocks.sortedWith(InAppContentBlockComparator.INSTANCE)
        Logger.i(this, "Got ${sortedContentBlocks.size} content blocks for placeholder $placeholderId")
        Logger.d(this, """
            Placeholder $placeholderId can show content blocks:
            ${ExponeaGson.instance.toJson(sortedContentBlocks)}
            """.trimIndent())
        // create unmutable copy due to updating of content blocks data while other loadings
        return sortedContentBlocks.firstOrNull()?.deepCopy()
    }

    private fun isStatusValid(contentBlock: InAppContentBlock): Boolean {
        if (contentBlock.isStatusValid()) {
            return true
        }
        Logger.i(this, """
            InAppCB: Block ${contentBlock.id} filtered out because of status ${contentBlock.status}
            """.trimIndent()
        )
        return false
    }

    private fun isContentSupportedToShow(contentBlock: InAppContentBlock): Boolean {
        if (SUPPORTED_CONTENT_BLOCK_TYPES_TO_SHOW.contains(contentBlock.contentType)) {
            return true
        }
        Logger.i(this, "InAppCB: Block ${contentBlock.id} content is unsupported to show")
        return false
    }

    private fun isContentSupportedToDownload(contentBlock: InAppContentBlock): Boolean {
        if (SUPPORTED_CONTENT_BLOCK_TYPES_TO_DOWNLOAD.contains(contentBlock.contentType)) {
            return true
        }
        Logger.i(this, "InAppCB: Block ${contentBlock.id} content is unsupported to download")
        return false
    }

    override fun passesFilters(contentBlock: InAppContentBlock): Boolean {
        Logger.i(this, "InAppCB: Validating filters for Content Block ${contentBlock.id}")
        return passesDateFilter(contentBlock) && passesFrequencyFilter(contentBlock)
    }

    override fun passesDateFilter(contentBlock: InAppContentBlock): Boolean {
        val dateFilterPass = contentBlock.applyDateFilter(System.currentTimeMillis() / 1000)
        Logger.i(this, "InAppCB: Block ${contentBlock.id} date-filter passed: $dateFilterPass")
        return dateFilterPass
    }

    override fun passesFrequencyFilter(contentBlock: InAppContentBlock): Boolean {
        val passessByFrequency = contentBlock.applyFrequencyFilter(
            displayStateRepository.get(contentBlock),
            sessionStartDate
        )
        Logger.i(this, "InAppCB: Block ${contentBlock.id} frequency-filter passed: $passessByFrequency")
        return passessByFrequency
    }

    override fun getAllInAppContentBlocksForPlaceholder(
        placeholderId: String
    ): List<InAppContentBlock> = runThreadSafelyWithResult {
        val contentBlocksForPlaceholder = contentBlocksData.filter { block ->
            block.placeholders.contains(placeholderId)
        }
        // ^ DEEPCOPY
        Logger.i(this,
            """InAppCB: ${contentBlocksForPlaceholder.size} blocks found for placeholder $placeholderId
            """.trimIndent()
        )
        Logger.d(this,
            """InAppCB: Found Content Blocks for placeholder $placeholderId:
            ${ExponeaGson.instance.toJson(contentBlocksForPlaceholder)}
            """.trimIndent()
        )
        return@runThreadSafelyWithResult contentBlocksForPlaceholder
    } ?: listOf()

    override fun loadInAppContentBlockPlaceholders() {
        Logger.d(this, "InAppCB: Loading of InApp Content Block placeholders requested")
        ensureOnBackgroundThread {
            dataAccess.waitForAccessWithDone { done ->
                Logger.d(this, "InAppCB: Loading of InApp Content Block placeholders starts")
                val customerIds = customerIdsRepository.get()
                val exponeaProject = projectFactory.mutualExponeaProject
                fetchManager.fetchStaticInAppContentBlocks(
                    exponeaProject = exponeaProject,
                    onSuccess = { result ->
                        val inAppContentBlocks = result.results ?: emptyList()
                        val supportedContentBlocks = inAppContentBlocks.filter {
                            isContentSupportedToDownload(it)
                        }
                        supportedContentBlocks.forEach {
                            it.customerIds = customerIds.toHashMap()
                        }
                        contentBlocksData = supportedContentBlocks
                        forceContentByPlaceholders(
                            supportedContentBlocks,
                            exponeaProject.inAppContentBlockPlaceholdersAutoLoad
                        ) {
                            Logger.i(this, "InAppCB: Block placeholders preloaded successfully")
                            done()
                        }
                    },
                    onFailure = {
                        Logger.e(
                            this,
                            "InAppCB: InApp Content Block placeholders failed. ${it.results.message}"
                        )
                        done()
                    }
                )
            }
        }
    }

    private fun forceContentByPlaceholders(
        target: List<InAppContentBlock>,
        autoLoadPlaceholders: List<String>,
        done: () -> Unit
    ) {
        Logger.i(
            this,
            "InAppCB: InApp Content Blocks prefetch starts for placeholders: $autoLoadPlaceholders"
        )
        val contentBlocksToLoad = target.filter {
            it.placeholders.intersect(autoLoadPlaceholders).isNotEmpty()
        }
        if (contentBlocksToLoad.isEmpty()) {
            Logger.i(this, "InAppCB: No InApp Content Block going to be prefetched")
            done()
            return
        }
        loadContentIfNeededAsync(contentBlocksToLoad) {
            done()
        }
    }

    override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {
        Logger.w(
            this,
            "InAppCB: Block ${contentBlock?.id ?: "no_ID"} has error $errorMessage"
        )
    }

    override fun onClose(placeholderId: String, contentBlock: InAppContentBlock) {
        Logger.i(this, "InAppCB: Block ${contentBlock.id} was closed")
        displayStateRepository.setInteracted(contentBlock, Date())
    }

    override fun onAction(
        placeholderId: String,
        contentBlock: InAppContentBlock,
        action: InAppContentBlockAction
    ) {
        Logger.i(this, "InAppCB: Block ${contentBlock.id} requested action ${action.name}")
        displayStateRepository.setInteracted(contentBlock, Date())
    }

    override fun onNoContent(placeholderId: String, contentBlock: InAppContentBlock?) {
        Logger.i(this, "InAppCB: Block ${contentBlock?.id ?: "no_ID"} has no content")
        contentBlock?.let {
            // possibility of AB testing
            displayStateRepository.setDisplayed(it, Date())
        }
    }

    override fun onShown(placeholderId: String, contentBlock: InAppContentBlock) {
        Logger.i(this, "InAppCB: Block ${contentBlock.id} has been shown")
        displayStateRepository.setDisplayed(contentBlock, Date())
    }

    override fun loadContent(placeholderId: String): InAppContentBlock? {
        val contentBlock = pickInAppContentBlock(placeholderId)
        if (contentBlock == null) {
            Logger.i(this, "InAppCB: No InApp Content Block found for placeholder $placeholderId")
        } else {
            Logger.i(
                this,
                "InAppCB: InApp Content Block ${contentBlock.id} for placeholder $placeholderId"
            )
            loadContentIfNeededSync(listOf(contentBlock))
        }
        return contentBlock
    }
}

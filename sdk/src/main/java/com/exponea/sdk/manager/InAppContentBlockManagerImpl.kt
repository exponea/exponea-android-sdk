package com.exponea.sdk.manager

import android.content.Context
import android.os.SystemClock
import com.exponea.sdk.Exponea
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
import com.exponea.sdk.repository.SimpleFileCache.Companion.DOWNLOAD_TIMEOUT_SECONDS
import com.exponea.sdk.services.IntegrationConfigFactory
import com.exponea.sdk.services.inappcontentblock.DefaultInAppContentCallback
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockActionDispatcher
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockComparator
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockDataLoader
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockViewController
import com.exponea.sdk.telemetry.model.TelemetryEvent
import com.exponea.sdk.util.ExponeaGson
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.ThreadSafeAccess
import com.exponea.sdk.util.currentTimeSeconds
import com.exponea.sdk.util.deepCopy
import com.exponea.sdk.util.ensureOnBackgroundThread
import com.exponea.sdk.view.InAppContentBlockPlaceholderView
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal class InAppContentBlockManagerImpl(
    private val displayStateRepository: InAppContentBlockDisplayStateRepository,
    private val fetchManager: FetchManager,
    private val integrationConfigFactory: IntegrationConfigFactory,
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
    @Volatile private var contentBlocksByPlaceholder: Map<String, List<InAppContentBlock>> = emptyMap()
    @Volatile private var contentBlocksById: Map<String, InAppContentBlock> = emptyMap()
    internal var contentBlocksData: List<InAppContentBlock> = emptyList()
        set(value) {
            field = value
            contentBlocksById = value.associateBy { it.id }
            val blocksByPlaceholder = hashMapOf<String, MutableList<InAppContentBlock>>()
            value.forEach { contentBlock ->
                contentBlock.placeholders.forEach { placeholder ->
                    blocksByPlaceholder.getOrPut(placeholder) { mutableListOf() }.add(contentBlock)
                }
            }
            contentBlocksByPlaceholder = blocksByPlaceholder
        }
    private val inFlightPersonalizedFetchLock = Any()
    private val inFlightPersonalizedFetchCallbacks =
        mutableMapOf<String, MutableList<PersonalizedFetchCallbacks>>()
    private val inFlightContentLoads = mutableMapOf<InFlightContentLoadKey, MutableList<(Boolean) -> Unit>>()
    // trying to display the same in a short time window, drop
    private val shownEventDedupWindowMs = 100L
    private val shownEventDedupLock = Any()
    private val shownEventRecent = mutableMapOf<String, Long>()

    internal val dataAccess = ThreadSafeAccess()

    private data class PersonalizedFetchCallbacks(
        val onSuccess: (List<InAppContentBlockPersonalizedData>) -> Unit,
        val onFailure: (FetchError) -> Unit
    )

    private data class InFlightContentLoadKey(
        val contentBlockId: String,
        val customerScope: String
    )

    override fun onEventCreated(event: Event, type: EventType) {
        when (type) {
            EventType.SESSION_START -> {
                Logger.d(this, "InAppCB: Event session_start occurs, storing time value")
                val eventTimestampInMillis = (event.timestamp ?: currentTimeSeconds()) * 1000
                sessionStartDate = Date(eventTimestampInMillis.toLong())
            }
            EventType.TRACK_CUSTOMER -> {
                if (Exponea.isStopped) {
                    Logger.e(this, "InAppCB: In-app content blocks fetch stopped, SDK is stopping")
                    return
                }
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
        clearRenderableCaches(contentBlocksData.map { it.id }.toSet())
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

    private fun updateContentForLocalContentBlocks(dataMap: Map<String, InAppContentBlockPersonalizedData>) {
        Logger.d(this, "InAppCB: Request to update personalized content of ${dataMap.keys.joinToString()}")
        dataMap.forEach { (blockId, personalizedData) ->
            contentBlocksById[blockId]?.personalizedData = personalizedData
        }
    }

    private fun clearRenderableCaches(contentBlockIds: Set<String>, clearResourceCaches: Boolean = true) {
        contentBlockIds.forEach { contentBlockId ->
            htmlCache.remove(contentBlockId)
        }
        if (clearResourceCaches) {
            imageCache.clear()
            fontCache.clear()
        }
    }

    private fun findStaticContentCacheInvalidations(
        currentContentBlocks: List<InAppContentBlock>,
        newContentBlocks: List<InAppContentBlock>
    ): Set<String> {
        if (currentContentBlocks.isEmpty()) return emptySet()
        val currentBlocksById = currentContentBlocks.associateBy { it.id }
        val newBlocksById = newContentBlocks.associateBy { it.id }
        val invalidatedIds = currentBlocksById.keys.minus(newBlocksById.keys).toMutableSet()
        newBlocksById.forEach { (blockId, newBlock) ->
            val currentBlock = currentBlocksById[blockId] ?: return@forEach
            if (currentBlock.contentType != newBlock.contentType || currentBlock.htmlContent != newBlock.htmlContent) {
                invalidatedIds.add(blockId)
            }
        }
        return invalidatedIds
    }

    private fun registerInFlightContentLoad(
        contentBlockIds: List<String>,
        customerIds: Map<String, String?>,
        completion: (Boolean) -> Unit
    ): List<String> {
        val requestedIds = contentBlockIds.distinct()
        if (requestedIds.isEmpty()) {
            return emptyList()
        }
        val customerScope = buildPersonalizedCustomerKey(customerIds)
        val pendingIdsCount = AtomicInteger(requestedIds.size)
        val allSucceeded = AtomicBoolean(true)
        val loadCompletion: (Boolean) -> Unit = { success ->
            if (!success) {
                allSucceeded.set(false)
            }
            if (pendingIdsCount.decrementAndGet() == 0) {
                completion(allSucceeded.get())
            }
        }
        val blockIdsToFetch = mutableListOf<String>()
        dataAccess.waitForAccess {
            requestedIds.forEach { blockId ->
                val key = InFlightContentLoadKey(blockId, customerScope)
                val existingLoads = inFlightContentLoads[key]
                if (existingLoads == null) {
                    inFlightContentLoads[key] = mutableListOf(loadCompletion)
                    blockIdsToFetch.add(blockId)
                } else {
                    existingLoads.add(loadCompletion)
                }
            }
        }
        return blockIdsToFetch
    }

    private fun completeInFlightContentLoad(
        contentBlockId: String,
        customerIds: Map<String, String?>,
        success: Boolean
    ) {
        val key = InFlightContentLoadKey(contentBlockId, buildPersonalizedCustomerKey(customerIds))
        val completions = dataAccess.waitForAccessWithResult {
            inFlightContentLoads.remove(key)?.toList() ?: emptyList()
        }.getOrElse {
            emptyList()
        }
        completions.forEach { completion ->
            completion(success)
        }
    }

    private fun completeInFlightContentLoad(
        contentBlockIds: List<String>,
        customerIds: Map<String, String?>,
        success: Boolean
    ) {
        contentBlockIds.distinct().forEach { blockId ->
            completeInFlightContentLoad(blockId, customerIds, success)
        }
    }

    private fun shouldTrackShownEvent(placeholderId: String, contentBlockId: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        val dedupeKey = "$placeholderId:$contentBlockId"
        synchronized(shownEventDedupLock) {
            val clearThreshold = shownEventDedupWindowMs * 5
            val iterator = shownEventRecent.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (now - entry.value > clearThreshold) {
                    iterator.remove()
                }
            }
            val previousShownAt = shownEventRecent[dedupeKey]
            if (previousShownAt != null && now - previousShownAt < shownEventDedupWindowMs) {
                return false
            }
            shownEventRecent[dedupeKey] = now
            return true
        }
    }

    /**
     * Loads missing or obsolete content for block.
     */
    override fun loadContentIfNeededSync(contentBlocks: List<InAppContentBlock>) {
        if (Exponea.isStopped) {
            Logger.e(this, "InAppCB: In-app content blocks fetch failed, SDK is stopping")
            return
        }
        val finished = CountDownLatch(1)
        loadContentIfNeededAsync(contentBlocks) {
            finished.countDown()
        }
        if (!finished.await(DOWNLOAD_TIMEOUT_SECONDS, SECONDS)) {
            Logger.e(this, "InAppCB: Content load has timed out")
        }
    }

    private fun loadContentIfNeededAsync(contentBlocks: List<InAppContentBlock>, done: () -> Unit) {
        val blockIdsToCheck = contentBlocks.map { it.id }.toSet()
        val blockIdsToUpdate = runThreadSafelyWithResult {
            contentBlocksData
                .filter { it.id in blockIdsToCheck && !it.hasFreshContent() }
                .map { it.id }
        } ?: emptyList()
        if (blockIdsToUpdate.isEmpty()) {
            Logger.d(this, "InAppCB: All content of blocks are fresh, nothing to update")
            done()
            return
        }
        if (Exponea.isStopped) {
            Logger.e(this, "InAppCB: In-app content blocks fetch failed, SDK is stopping")
            done()
            return
        }
        val customerIdsSnapshot = customerIdsRepository.get()
        val customerIdsMap = customerIdsSnapshot.toHashMap()
        val blockIdsToFetch = registerInFlightContentLoad(blockIdsToUpdate, customerIdsMap) { _ ->
            done()
        }
        if (blockIdsToFetch.isEmpty()) {
            Logger.d(this, "InAppCB: Joining in-flight content load for blocks: ${blockIdsToUpdate.joinToString()}")
            return
        }
        val joinedBlockIds = blockIdsToUpdate.distinct().filterNot { it in blockIdsToFetch }
        if (joinedBlockIds.isNotEmpty()) {
            Logger.d(this, "InAppCB: Joining in-flight content load for blocks: ${joinedBlockIds.joinToString()}")
        }
        Logger.i(this, "InAppCB: Loading content for blocks: ${blockIdsToFetch.joinToString()}")
        if (Exponea.isStopped) {
            Logger.e(this, "InAppCB: In-app content blocks fetch failed, SDK is stopping")
            completeInFlightContentLoad(blockIdsToFetch, customerIdsMap, false)
            return
        }
        val personalizedFetchKey = buildPersonalizedFetchKey(customerIdsMap, blockIdsToFetch)
        val shouldStartFetch = addInFlightPersonalizedFetchCallback(
            personalizedFetchKey,
            PersonalizedFetchCallbacks(
                onSuccess = onPersonalizedSuccess@{ contentData ->
                    if (Exponea.isStopped) {
                        Logger.e(this, "InAppCB: In-app content blocks fetch failed, SDK is stopping")
                        completeInFlightContentLoad(blockIdsToFetch, customerIdsMap, false)
                        return@onPersonalizedSuccess
                    }
                    val dataMap = contentData.associateBy { it.blockId }
                    // update personalized data for requested 'contentBlocks'
                    val updateContentBlocks = mutableListOf<InAppContentBlock>()
                    var cacheInvalidationIds = emptySet<String>()
                    dataAccess.waitForAccess {
                        cacheInvalidationIds = contentBlocksData
                            .filter { it.id in blockIdsToFetch && it.personalizedData != null }
                            .map { it.id }
                            .toSet()
                        contentBlocks.forEach { contentBlock ->
                            dataMap[contentBlock.id]?.let {
                                contentBlock.personalizedData = it
                                updateContentBlocks.add(contentBlock)
                            }
                        }
                        // update personalized data for local 'contentBlocksData'
                        updateContentForLocalContentBlocks(dataMap)
                    }
                    if (cacheInvalidationIds.isNotEmpty()) {
                        Logger.d(
                            this,
                            "InAppCB: Clearing render cache for refreshed personalized blocks: " +
                                cacheInvalidationIds.joinToString()
                        )
                        clearRenderableCaches(cacheInvalidationIds)
                    }
                    Exponea.telemetry?.reportEvent(TelemetryEvent.CONTENT_BLOCK_PERSONALISED_FETCH, hashMapOf(
                        "count" to contentData.size.toString(),
                        "data" to ExponeaGson.instance.toJson(updateContentBlocks.map {
                            mapOf(
                                "messageId" to it.id,
                                "placeholders" to it.placeholders,
                                "type" to if (it.isContentPersonalized()) "personal" else "static"
                            )
                        })
                    ))
                    completeInFlightContentLoad(blockIdsToFetch, customerIdsMap, true)
                },
                onFailure = {
                    completeInFlightContentLoad(blockIdsToFetch, customerIdsMap, false)
                }
            )
        )
        if (!shouldStartFetch) {
            Logger.d(
                this,
                "InAppCB: Joining in-flight personalized fetch for blocks ${blockIdsToFetch.joinToString()}"
            )
            return
        }
        Logger.i(this, "InAppCB: Prefetching personalized content for current customer")
        val contentBlockIdsAsString = blockIdsToFetch.joinToString()
        fetchManager.fetchPersonalizedContentBlocks(
            integrationConfig = integrationConfigFactory.integrationConfig,
            customerIds = customerIdsSnapshot,
            contentBlockIds = blockIdsToFetch,
            onSuccess = { result ->
                if (Exponea.isStopped) {
                    Logger.e(this, "InAppCB: In-app content blocks fetch failed, SDK is stopping")
                    notifyInFlightPersonalizedFetchFailure(
                        personalizedFetchKey,
                        FetchError(null, "SDK is stopping")
                    )
                    return@fetchPersonalizedContentBlocks
                }
                Logger.i(
                    this,
                    "InAppCB: Personalized content for blocks $contentBlockIdsAsString loaded"
                )
                val data = result.results ?: emptyList()
                data.forEach {
                    it.loadedAt = Date()
                }
                notifyInFlightPersonalizedFetchSuccess(personalizedFetchKey, data)
            },
            onFailure = {
                val errorMessage = it.results.message
                Logger.e(
                    this,
                    "InAppCB: Personalized content for blocks $contentBlockIdsAsString failed: $errorMessage"
                )
                notifyInFlightPersonalizedFetchFailure(personalizedFetchKey, it.results)
            }
        )
    }

    private fun buildPersonalizedFetchKey(
        customerIds: Map<String, String?>,
        contentBlockIds: List<String>
    ): String {
        val customerScope = buildPersonalizedCustomerKey(customerIds)
        val sortedBlockIds = contentBlockIds.sorted().joinToString(",")
        return "$customerScope|$sortedBlockIds"
    }

    private fun buildPersonalizedCustomerKey(customerIds: Map<String, String?>): String {
        val sortedCustomerIds = customerIds
            .toList()
            .sortedBy { it.first }
            .joinToString("&") { (key, value) -> "$key=${value ?: ""}" }
        return sortedCustomerIds
    }

    private fun addInFlightPersonalizedFetchCallback(
        key: String,
        callbacks: PersonalizedFetchCallbacks
    ): Boolean = synchronized(inFlightPersonalizedFetchLock) {
        val listeners = inFlightPersonalizedFetchCallbacks.getOrPut(key) { mutableListOf() }
        listeners.add(callbacks)
        listeners.size == 1
    }

    private fun popInFlightPersonalizedFetchCallbacks(key: String): List<PersonalizedFetchCallbacks> {
        return synchronized(inFlightPersonalizedFetchLock) {
            inFlightPersonalizedFetchCallbacks.remove(key)?.toList() ?: emptyList()
        }
    }

    private fun notifyInFlightPersonalizedFetchSuccess(
        key: String,
        data: List<InAppContentBlockPersonalizedData>
    ) {
        popInFlightPersonalizedFetchCallbacks(key).forEach { callbacks ->
            callbacks.onSuccess(data)
        }
    }

    private fun notifyInFlightPersonalizedFetchFailure(
        key: String,
        error: FetchError
    ) {
        popInFlightPersonalizedFetchCallbacks(key).forEach { callbacks ->
            callbacks.onFailure(error)
        }
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

    override fun onIntegrationStopped() {
        clearAll()
        imageCache.clear()
        htmlCache.clearAll()
        fontCache.clear()
        displayStateRepository.clear()
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
        val contentBlocksForPlaceholder = contentBlocksByPlaceholder[placeholderId] ?: emptyList()
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

    override fun loadInAppContentBlockPlaceholders(inAppContentBlockPlaceholdersAutoLoad: List<String>) {
        if (Exponea.isStopped) {
            Logger.e(this, "InAppCB: In-app content blocks fetch failed, SDK is stopping")
            return
        }
        Logger.d(this, "InAppCB: Loading of InApp Content Block placeholders requested")
        ensureOnBackgroundThread {
            if (Exponea.isStopped) {
                Logger.e(this, "InAppCB: In-app content blocks fetch failed, SDK is stopping")
                return@ensureOnBackgroundThread
            }
            Logger.d(this, "InAppCB: Loading of InApp Content Block placeholders starts")
            val customerIds = customerIdsRepository.get().toHashMap()
            fetchManager.fetchStaticInAppContentBlocks(
                integrationConfig = integrationConfigFactory.integrationConfig,
                onSuccess = { result ->
                    if (Exponea.isStopped) {
                        Logger.e(this, "InAppCB: In-app content blocks fetch failed, SDK is stopping")
                        return@fetchStaticInAppContentBlocks
                    }
                    val inAppContentBlocks = result.results ?: emptyList()
                    Exponea.telemetry?.reportEvent(TelemetryEvent.CONTENT_BLOCK_INIT_FETCH, hashMapOf(
                        "count" to inAppContentBlocks.size.toString(),
                        "data" to ExponeaGson.instance.toJson(inAppContentBlocks.map {
                            mapOf(
                                "messageId" to it.id,
                                "placeholders" to it.placeholders,
                                "type" to if (it.isContentPersonalized()) "personal" else "static"
                            )
                        })
                    ))
                    val supportedContentBlocks = inAppContentBlocks.filter {
                        isContentSupportedToDownload(it)
                    }
                    supportedContentBlocks.forEach {
                        it.customerIds = customerIds
                    }
                    var invalidatedCacheIds = emptySet<String>()
                    dataAccess.waitForAccess {
                        invalidatedCacheIds = findStaticContentCacheInvalidations(
                            contentBlocksData,
                            supportedContentBlocks
                        )
                        contentBlocksData = supportedContentBlocks
                    }
                    if (invalidatedCacheIds.isNotEmpty()) {
                        Logger.d(
                            this,
                            "InAppCB: Clearing render cache for updated static blocks: " +
                                invalidatedCacheIds.joinToString()
                        )
                        clearRenderableCaches(invalidatedCacheIds)
                    }
                    forceContentByPlaceholders(
                        supportedContentBlocks,
                        inAppContentBlockPlaceholdersAutoLoad
                    ) {
                        Logger.i(this, "InAppCB: Block placeholders preloaded successfully")
                    }
                },
                onFailure = {
                    Logger.e(
                        this,
                        "InAppCB: InApp Content Block placeholders failed. ${it.results.message}"
                    )
                }
            )
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
        if (!shouldTrackShownEvent(placeholderId, contentBlock.id)) {
            Logger.d(
                this,
                "InAppCB: Duplicate shown suppressed for block ${contentBlock.id} in placeholder $placeholderId"
            )
            return
        }
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
        }
        return contentBlock
    }
}

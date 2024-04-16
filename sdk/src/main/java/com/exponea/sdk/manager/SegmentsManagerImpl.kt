package com.exponea.sdk.manager

import androidx.annotation.WorkerThread
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.Segment
import com.exponea.sdk.models.SegmentationCategories
import com.exponea.sdk.models.SegmentationData
import com.exponea.sdk.models.SegmentationDataCallback
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.SegmentsCache
import com.exponea.sdk.services.ExponeaProjectFactory
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.ensureOnBackgroundThread
import com.exponea.sdk.util.runOnBackgroundThread
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Job

internal class SegmentsManagerImpl(
    private val fetchManager: FetchManager,
    private val projectFactory: ExponeaProjectFactory,
    private val customerIdsRepository: CustomerIdsRepository,
    private val segmentsCache: SegmentsCache
) : SegmentsManager {

    companion object {
        internal var CHECK_DEBOUNCE_MILLIS: Long = if (Exponea.isUnitTest()) 1000L else 5000L
    }

    internal var checkSegmentsJob: Job? = null

    internal val newbieCallbacks = CopyOnWriteArrayList<SegmentationDataCallback>()

    override fun onEventUploaded(event: ExportedEvent) {
        if (areCallbacksInactive()) {
            Logger.v(
                this,
                """
                Segments: Skipping segments update process after tracked event due to no callback registered
                """.trimIndent()
            )
            return
        }
        triggerSegmentsChangeCheck()
    }

    override fun onCallbackAdded(callback: SegmentationDataCallback) {
        if (areCallbacksInactive()) {
            // Callback registration triggers this but list of callbacks has been cleared meanwhile
            Logger.e(this, "Segments: Adding of callback triggers fetch for no callbacks registered")
            return
        }
        if (callback.includeFirstLoad) {
            newbieCallbacks.add(callback)
        }
        triggerSegmentsChangeCheck()
    }

    override fun reload() {
        if (areCallbacksInactive()) {
            Logger.v(this, "Segments: Skipping segments reload process for no callback")
            return
        }
        triggerSegmentsChangeCheck()
    }

    override fun onSdkInit() {
        if (areCallbacksInactive()) {
            Logger.d(this, "Segments: Skipping initial segments update process for no callback")
            return
        }
        val firstLoadCallbacks = Exponea.segmentationDataCallbacks.filter { it.includeFirstLoad }
        if (firstLoadCallbacks.isEmpty()) {
            Logger.d(this, "Segments: Skipping initial segments update process as is not required")
            return
        }
        newbieCallbacks.addAll(firstLoadCallbacks)
        triggerSegmentsChangeCheck()
    }

    private fun triggerSegmentsChangeCheck() {
        cancelSegmentsFetchJob()
        val customerIdsForFetch = customerIdsRepository.get()
        checkSegmentsJob = runOnBackgroundThread(CHECK_DEBOUNCE_MILLIS) {
            if (checkSegmentsJob == null || areCallbacksInactive()) {
                Logger.w(this, "Segments: Segments change check has been cancelled meanwhile")
            } else {
                runSegmentsChangeCheck(
                    customerIdsForFetch,
                    popNewbieCallbacks()
                )
            }
        }
    }

    private fun popNewbieCallbacks(): List<SegmentationDataCallback> {
        val popAll = newbieCallbacks.toList()
        newbieCallbacks.clear()
        return popAll
    }

    @WorkerThread
    private fun runSegmentsChangeCheck(
        triggeringCustomerIds: CustomerIds,
        forceNotifyCallbacks: List<SegmentationDataCallback>
    ) {
        if (!areCustomerIdsActual(triggeringCustomerIds)) {
            Logger.w(
                this,
                "Segments: Check process was canceled because customer has changed"
            )
            return
        }
        if (customerIdsMergeIsRequired(triggeringCustomerIds)) {
            Logger.i(this, "Segments: Current customer IDs require to be linked")
            val mergeResult = fetchManager.linkCustomerIdsSync(
                projectFactory.mainExponeaProject,
                triggeringCustomerIds
            )
            if (mergeResult.success != true) {
                Logger.e(
                    this,
                    "Segments: Customer IDs $triggeringCustomerIds merge failed, unable to fetch segments"
                )
                return
            }
        }
        fetchManager.fetchSegments(
            exponeaProject = projectFactory.mainExponeaProject,
            customerIds = triggeringCustomerIds,
            onSuccess = {
                checkSegmentsJob = null
                val data = it.results
                Logger.i(
                    this,
                    "Segments: Data loaded successfully, size: ${data.size}"
                )
                if (areCustomerIdsActual(triggeringCustomerIds)) {
                    handleFetchedSegmentationsData(data, triggeringCustomerIds, forceNotifyCallbacks)
                } else {
                    Logger.w(
                        this,
                        "Segments: New data are ignored because were loaded for different customer"
                    )
                }
            },
            onFailure = {
                checkSegmentsJob = null
                val errorMessage = it.results.message
                Logger.e(
                    this,
                    "Segments: Fetch of segments failed: $errorMessage"
                )
            }
        )
    }

    private fun customerIdsMergeIsRequired(customerIds: CustomerIds): Boolean {
        return segmentsCache.get()?.customerIds != customerIds && customerIds.externalIds.isNotEmpty()
    }

    private fun areCustomerIdsActual(customerIds: CustomerIds): Boolean {
        return customerIdsRepository.get().toHashMap() == customerIds.toHashMap()
    }

    private fun handleFetchedSegmentationsData(
        newData: SegmentationCategories,
        newCustomerIds: CustomerIds,
        forceNotifyCallbacks: List<SegmentationDataCallback>
    ) {
        val newSegmentsData = SegmentationData(
            customerIds = newCustomerIds,
            segmentations = newData
        )
        val syncResult = synchronizeSegments(newSegmentsData)
        Logger.i(this, "Segments: Diff detected for segmentations ${syncResult.diffs}")
        for (categoryDiff in syncResult.diffs) {
            val categoryName = categoryDiff.key
            val newSegmentsForCategory = (newData[categoryName] ?: emptyList()).map {
                it.deepClone()
            }
            Exponea.getSegmentationDataCallbacks(categoryName).forEach {
                if (shouldNotifyCallback(it, categoryDiff.value, forceNotifyCallbacks)) {
                    ensureOnBackgroundThread {
                        it.onNewData(newSegmentsForCategory)
                    }
                }
            }
        }
    }

    private fun shouldNotifyCallback(
        callback: SegmentationDataCallback,
        diff: Diff,
        forceNotifyCallbacks: List<SegmentationDataCallback>
    ): Boolean {
        val callbackIsNewbie = forceNotifyCallbacks.contains(callback)
        if (callbackIsNewbie) {
            return true
        }
        return diff != Diff.SAME
    }

    internal enum class Diff {
        SAME,
        CHANGE,
        NEW,
        OLD
    }
    private data class SyncResult(
        val diffs: Map<String, Diff>
    ) {
        fun changedCategories(): List<String> {
            return diffs
                .filter { it.value != Diff.SAME }
                .map { it.key }
        }

        fun changesDetected(): Boolean {
            return changedCategories().isNotEmpty()
        }
    }

    @WorkerThread
    private fun synchronizeSegments(newSegmentsData: SegmentationData): SyncResult {
        return synchronized(this) {
            val currentSegmentationData = segmentsCache.get()
            if (currentSegmentationData?.customerIds != newSegmentsData.customerIds) {
                // all data are obsolete
                Logger.d(
                    this,
                    "Segments: Customer IDs changed, all data are considered as new"
                )
                segmentsCache.set(newSegmentsData)
                return@synchronized SyncResult(
                    newSegmentsData.segmentations.mapValues { Diff.NEW }
                )
            }
            val currentSegmentations = currentSegmentationData.segmentations
            val newSegmentations = newSegmentsData.segmentations
            val categoryNamesUnion: Set<String> = currentSegmentations.plus(newSegmentations)
                .map { it.key }
                .toSet()
            val syncResult = SyncResult(
                categoryNamesUnion.associateWith { detectChangeType(currentSegmentations[it], newSegmentations[it]) }
            )
            if (syncResult.changesDetected()) {
                Logger.d(
                    this,
                    "Segments: Storing data because of changed ${syncResult.changedCategories()}"
                )
                segmentsCache.set(newSegmentsData)
            }
            return@synchronized syncResult
        }
    }

    private fun detectChangeType(previous: List<Segment>?, next: List<Segment>?): Diff {
        return when {
            previous == next -> Diff.SAME
            previous == null -> Diff.NEW
            next == null -> Diff.OLD
            else -> Diff.CHANGE
        }
    }

    override fun clearAll() {
        cancelSegmentsFetchJob()
        segmentsCache.clear()
        newbieCallbacks.clear()
    }

    private fun cancelSegmentsFetchJob() {
        checkSegmentsJob?.cancel()
        checkSegmentsJob = null
    }

    private fun areCallbacksInactive(): Boolean = Exponea.segmentationDataCallbacks.isEmpty()
}

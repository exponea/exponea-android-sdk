package com.exponea.sdk.models

import androidx.annotation.WorkerThread
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockCarouselComparator

open class ContentBlockSelector {
    @WorkerThread
    open fun filterContentBlocks(source: List<InAppContentBlock>): List<InAppContentBlock> {
        return source
    }
    @WorkerThread
    open fun sortContentBlocks(source: List<InAppContentBlock>): List<InAppContentBlock> {
        return source.sortedWith(InAppContentBlockCarouselComparator.INSTANCE)
    }
}

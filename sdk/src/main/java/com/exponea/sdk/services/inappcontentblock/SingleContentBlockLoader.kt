package com.exponea.sdk.services.inappcontentblock

import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.util.Logger
import java.util.concurrent.atomic.AtomicReference

internal class SingleContentBlockLoader : InAppContentBlockDataLoader {
    private val assignedContentBlockRef = AtomicReference<InAppContentBlock?>(null)
    internal var assignedContentBlock: InAppContentBlock?
        get() = assignedContentBlockRef.get()
        set(value) {
            assignedContentBlockRef.set(value)
        }

    override fun loadContent(placeholderId: String): InAppContentBlock? {
        val contentBlock = assignedContentBlockRef.get()
        if (contentBlock == null) {
            Logger.w(
                this,
                "InAppCb: Content block loader has been requested for non-assigned placeholder: $placeholderId"
            )
        }
        return contentBlock
    }
}

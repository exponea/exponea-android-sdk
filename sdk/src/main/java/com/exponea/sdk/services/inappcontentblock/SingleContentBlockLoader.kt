package com.exponea.sdk.services.inappcontentblock

import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.util.Logger

internal class SingleContentBlockLoader : InAppContentBlockDataLoader {
    internal var assignedContentBlock: InAppContentBlock? = null
    override fun loadContent(placeholderId: String): InAppContentBlock? {
        assignedContentBlock?.let {
            Logger.w(
                this,
                "InAppCb: Content block loader has been requested for non-assigned placeholder: $placeholderId"
            )
        }
        return assignedContentBlock
    }
}

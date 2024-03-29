package com.exponea.sdk.services.inappcontentblock

import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction

interface InAppContentBlockActionDispatcher {
    fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String)
    fun onClose(placeholderId: String, contentBlock: InAppContentBlock)
    fun onAction(
        placeholderId: String,
        contentBlock: InAppContentBlock,
        action: InAppContentBlockAction
    )
    fun onNoContent(placeholderId: String, contentBlock: InAppContentBlock?)
    fun onShown(placeholderId: String, contentBlock: InAppContentBlock)
}

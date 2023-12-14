package com.exponea.sdk.models

interface InAppContentBlockCallback {
    fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock)
    fun onNoMessageFound(placeholderId: String)
    fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String)
    fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock)
    fun onActionClicked(placeholderId: String, contentBlock: InAppContentBlock, action: InAppContentBlockAction)
}

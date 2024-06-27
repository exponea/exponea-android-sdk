package com.exponea.sdk.models

interface ContentBlockCarouselCallback {
    /**
     * Triggered when a content block is shown to user.
     * Parameter 'index' is zero-based index of the shown content block.
     * Parameter 'count' is total number of content blocks available to user.
     */
    fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock, index: Int, count: Int)

    /**
     * Triggered when a list of content blocks is reloaded.
     * Parameter 'count' is total number of content blocks available to user.
     * Parameter 'messages' is list of all content blocks.
     */
    fun onMessagesChanged(count: Int, messages: List<InAppContentBlock>)
}

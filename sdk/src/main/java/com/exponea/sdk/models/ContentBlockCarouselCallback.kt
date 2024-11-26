package com.exponea.sdk.models

interface ContentBlockCarouselCallback {

    /**
     * If overrideDefaultBehavior is set to true, default action will not be performed (deep link, universal link, etc.)
     **/
    val overrideDefaultBehavior: Boolean

    /**
     * If trackActions is set to false, click and close in-app content block events will not be tracked automatically
     **/
    val trackActions: Boolean

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

    /**
     * Triggered when no content block has been found for a given placeholder.
     */
    fun onNoMessageFound(placeholderId: String)

    /**
     * Triggered when an error occurs while loading or showing of content blocks.
     * Parameter `contentBlock` is the content block which caused the error or null in case of general problem.
     * Parameter `errorMessage` is the error message that describes the problem.
     */
    fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String)

    /**
     * Triggered when a content block is closed.
     */
    fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock)

    /**
     * Triggered when a content block action is clicked.
     * Parameter `action` contains the action information.
     */
    fun onActionClicked(placeholderId: String, contentBlock: InAppContentBlock, action: InAppContentBlockAction)

    /**
     * Triggered when a carousel changed its height.
     */
    fun onHeightUpdate(placeholderId: String, height: Int)
}

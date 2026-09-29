package com.exponea.example.contentblocks

import android.util.Log
import com.exponea.sdk.models.ContentBlockCarouselCallback
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction

object CarouselCallbackFactory {

    private val tag = this::class.simpleName

    fun createDefault(onStatusChanged: (String) -> Unit): ContentBlockCarouselCallback {
        return object : ContentBlockCarouselCallback {
            override val overrideDefaultBehavior = false
            override val trackActions = true

            private var count = 0
            private var index = -1
            private var blockName: String? = null

            override fun onMessageShown(
                placeholderId: String,
                contentBlock: InAppContentBlock,
                index: Int,
                count: Int
            ) {
                blockName = contentBlock.name
                this.index = index
                this.count = count
                updateStatus(onStatusChanged)
            }

            override fun onMessagesChanged(count: Int, messages: List<InAppContentBlock>) {
                if (count == 0) {
                    blockName = null
                    index = -1
                }
                this.count = count
                updateStatus(onStatusChanged)
            }

            override fun onNoMessageFound(placeholderId: String) {
                onStatusChanged("Carousel $placeholderId is empty")
            }

            override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {
                Log.e(tag, "Carousel $placeholderId error: $errorMessage")
            }

            override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) {
                Log.i(tag, "Message ${contentBlock.name} closed in carousel $placeholderId")
            }

            override fun onActionClicked(
                placeholderId: String,
                contentBlock: InAppContentBlock,
                action: InAppContentBlockAction
            ) {
                Log.i(tag, "Action ${action.name} clicked in carousel $placeholderId")
            }

            override fun onHeightUpdate(placeholderId: String, height: Int) {
                Log.i(tag, "Carousel $placeholderId height $height")
            }

            private fun updateStatus(onStatusChanged: (String) -> Unit) {
                onStatusChanged(
                    if (count == 0) {
                        "Carousel is empty"
                    } else {
                        "Showing ${blockName ?: ""} as ${index + 1} of $count"
                    }
                )
            }
        }
    }
}

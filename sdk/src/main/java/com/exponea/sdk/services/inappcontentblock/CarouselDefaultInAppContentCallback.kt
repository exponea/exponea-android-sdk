package com.exponea.sdk.services.inappcontentblock

import android.content.Context
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.util.Logger

class CarouselDefaultInAppContentCallback(context: Context) : DefaultInAppContentCallback(context) {
    override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
        Logger.d(this, "InAppCbCarousel: Tracking of InApp Content Block ${contentBlock.id} show")
        Exponea.trackInAppContentBlockShown(placeholderId, contentBlock)
        Exponea.telemetry?.reportEvent(
            com.exponea.sdk.telemetry.model.EventType.SHOW_IN_APP_MESSAGE,
            hashMapOf("messageType" to "content_block_carousel")
        )
    }
}

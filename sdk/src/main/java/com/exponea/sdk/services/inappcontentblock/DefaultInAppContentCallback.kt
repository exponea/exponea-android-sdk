package com.exponea.sdk.services.inappcontentblock

import android.content.Context
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.models.InAppContentBlockActionType
import com.exponea.sdk.models.InAppContentBlockCallback
import com.exponea.sdk.telemetry.model.TelemetryEvent
import com.exponea.sdk.util.ExponeaGson
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.UrlOpener

open class DefaultInAppContentCallback(private val context: Context) : InAppContentBlockCallback {
    override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
        Logger.d(this, "Tracking of InApp Content Block ${contentBlock.id} show")
        Exponea.trackInAppContentBlockShown(placeholderId, contentBlock)
        Exponea.telemetry?.reportEvent(TelemetryEvent.CONTENT_BLOCK_SHOWN, hashMapOf(
            "type" to "static",
            "messageId" to contentBlock.id,
            "placeholders" to ExponeaGson.instance.toJson(contentBlock.placeholders)
        ))
    }

    override fun onNoMessageFound(placeholderId: String) {
        Logger.i(this, "InApp Content Block has no content for $placeholderId")
    }

    override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {
        if (contentBlock == null) {
            Logger.e(this, "InApp Content Block is empty!!! Nothing to track")
            return
        }
        Logger.d(this, "Tracking of InApp Content Block ${contentBlock.id} error")
        Exponea.trackInAppContentBlockError(placeholderId, contentBlock, errorMessage)
    }

    override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) {
        Logger.d(this, "Tracking of InApp Content Block ${contentBlock.id} close")
        Exponea.trackInAppContentBlockClose(placeholderId, contentBlock)
    }

    override fun onActionClicked(
        placeholderId: String,
        contentBlock: InAppContentBlock,
        action: InAppContentBlockAction
    ) {
        Logger.d(
                this,
                "Tracking of InApp Content Block ${contentBlock.id} action ${action.name}"
        )
        Exponea.trackInAppContentBlockClick(placeholderId, action, contentBlock)
        invokeAction(action)
    }

    private fun invokeAction(action: InAppContentBlockAction) {
        Logger.d(
                this,
                "Invoking InApp Content Block action '${action.name}'"
        )
        if (action.type == InAppContentBlockActionType.CLOSE) {
            return
        }

        UrlOpener.openUrlInApp(context = context, url = action.url)
    }
}

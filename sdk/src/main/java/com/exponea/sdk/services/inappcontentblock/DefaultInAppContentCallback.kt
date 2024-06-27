package com.exponea.sdk.services.inappcontentblock

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.models.InAppContentBlockActionType
import com.exponea.sdk.models.InAppContentBlockCallback
import com.exponea.sdk.util.Logger

open class DefaultInAppContentCallback(private val context: Context) : InAppContentBlockCallback {
    override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
        Logger.d(this, "Tracking of InApp Content Block ${contentBlock.id} show")
        Exponea.trackInAppContentBlockShown(placeholderId, contentBlock)
        Exponea.telemetry?.reportEvent(
            com.exponea.sdk.telemetry.model.EventType.SHOW_IN_APP_MESSAGE,
            hashMapOf("messageType" to "content_block")
        )
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
        val actionUrl = try {
            Uri.parse(action.url)
        } catch (e: Exception) {
            Logger.e(
                    this,
                    "InApp Content Block action ${action.url} is invalid",
                    e
            )
            return
        }
        try {
            context.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        data = actionUrl
                    }
            )
        } catch (e: ActivityNotFoundException) {
            Logger.e(this, "InApp Content Block action failed", e)
        }
    }
}

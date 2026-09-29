package com.exponea.example.contentblocks

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.models.InAppContentBlockCallback
import com.exponea.sdk.util.HtmlNormalizer

object ContentBlockCallbackFactory {

    private val tag = this::class.simpleName

    fun createExampleTopCallback(
        context: Context,
        original: InAppContentBlockCallback?
    ): InAppContentBlockCallback {
        val base = original ?: emptyCallback()
        return object : InAppContentBlockCallback {
            override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
                base.onMessageShown(placeholderId, contentBlock)
                Log.i(tag, "Content block with HTML: ${contentBlock.htmlContent}")
                contentBlock.htmlContent?.let { rawHtml ->
                    val normalizedHtml = HtmlNormalizer(
                        context,
                        rawHtml
                    ).normalize(
                        HtmlNormalizer.HtmlNormalizerConfig(
                            makeResourcesOffline = true,
                            ensureCloseButton = false
                        )
                    ).html
                    Log.i(tag, "Normalized HTML: $normalizedHtml")
                }
            }

            override fun onNoMessageFound(placeholderId: String) {
                base.onNoMessageFound(placeholderId)
            }

            override fun onError(
                placeholderId: String,
                contentBlock: InAppContentBlock?,
                errorMessage: String
            ) {
                if (contentBlock != null) {
                    Exponea.trackInAppContentBlockErrorWithoutTrackingConsent(
                        placeholderId,
                        contentBlock,
                        errorMessage
                    )
                }
                base.onError(placeholderId, contentBlock, errorMessage)
            }

            override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) {
                Exponea.trackInAppContentBlockCloseWithoutTrackingConsent(placeholderId, contentBlock)
                base.onCloseClicked(placeholderId, contentBlock)
            }

            override fun onActionClicked(
                placeholderId: String,
                contentBlock: InAppContentBlock,
                action: InAppContentBlockAction
            ) {
                Exponea.trackInAppContentBlockClickWithoutTrackingConsent(
                    placeholderId,
                    action,
                    contentBlock
                )
                base.onActionClicked(placeholderId, contentBlock, action)
                openUrl(context, action.url)
            }
        }
    }

    private fun openUrl(context: Context, url: String?) {
        if (url.isNullOrBlank()) return
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    data = Uri.parse(url)
                }
            )
        } catch (e: ActivityNotFoundException) {
            Log.e(tag, "Unable to perform deeplink", e)
        }
    }

    private fun emptyCallback() = object : InAppContentBlockCallback {
        override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {}
        override fun onNoMessageFound(placeholderId: String) {}
        override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {}
        override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) {}
        override fun onActionClicked(
            placeholderId: String,
            contentBlock: InAppContentBlock,
            action: InAppContentBlockAction
        ) {}
    }
}

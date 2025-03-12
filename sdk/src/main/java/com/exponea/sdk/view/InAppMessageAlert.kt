package com.exponea.sdk.view

import android.app.AlertDialog
import android.content.Context
import com.exponea.sdk.models.InAppMessageButtonType
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.models.InAppMessagePayloadButton
import com.exponea.sdk.util.Logger

internal class InAppMessageAlert(
    context: Context,
    payload: InAppMessagePayload,
    onButtonClick: (InAppMessagePayloadButton) -> Unit,
    onDismiss: (Boolean, InAppMessagePayloadButton?) -> Unit,
    private val onError: (String) -> Unit
) : InAppMessageView {
    val dialog: AlertDialog
    var userInteraction: Boolean = false
    override val isPresented: Boolean
        get() = dialog.isShowing

    init {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(payload.title)
        builder.setMessage(payload.bodyText)
        if (payload.buttons != null) {
            for (button in payload.buttons) {
                if (button.buttonType == InAppMessageButtonType.CANCEL) {
                    builder.setNegativeButton(button.text) { _, _ ->
                        userInteraction = true
                        onDismiss(true, button)
                        dismiss()
                    }
                } else {
                    builder.setPositiveButton(button.text) { _, _ ->
                        userInteraction = true
                        onButtonClick(button)
                    }
                }
            }
        }
        builder.setOnDismissListener {
            if (!userInteraction) {
                onDismiss(false, null)
            }
        }
        dialog = builder.create()
    }

    override fun show() {
        try {
            dialog.show()
        } catch (e: Exception) {
            Logger.e(this, "[InApp] Unable to show Alert in-app message", e)
            onError.invoke("Invalid app foreground state")
        }
    }

    override fun dismiss() {
        try {
            dialog.dismiss()
        } catch (e: Exception) {
            Logger.e(this, "[InApp] Dismissing Alert in-app message failed", e)
        }
    }
}

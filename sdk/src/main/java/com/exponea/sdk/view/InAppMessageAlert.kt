package com.exponea.sdk.view

import android.app.AlertDialog
import android.content.Context
import com.exponea.sdk.models.InAppMessageButtonType
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.models.InAppMessagePayloadButton

internal class InAppMessageAlert : InAppMessageView {
    val dialog: AlertDialog
    var userInteraction: Boolean = false
    override val isPresented: Boolean
        get() = dialog.isShowing

    constructor(
        context: Context,
        payload: InAppMessagePayload,
        onButtonClick: (InAppMessagePayloadButton) -> Unit,
        onDismiss: (Boolean) -> Unit
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(payload.title)
        builder.setMessage(payload.bodyText)
        if (payload.buttons != null) {
            for (button in payload.buttons) {
                if (button.buttonType == InAppMessageButtonType.CANCEL) {
                    builder.setNegativeButton(button.buttonText) { _, _ ->
                        userInteraction = true
                        onDismiss(true)
                        dismiss()
                    }
                } else {
                    builder.setPositiveButton(button.buttonText) { _, _ ->
                        userInteraction = true
                        onButtonClick(button)
                    }
                }
            }
        }
        builder.setOnDismissListener {
            if (!userInteraction) {
                onDismiss(false)
            }
        }
        dialog = builder.create()
    }

    override fun show() {
        dialog.show()
    }

    override fun dismiss() {
        dialog.dismiss()
    }
}

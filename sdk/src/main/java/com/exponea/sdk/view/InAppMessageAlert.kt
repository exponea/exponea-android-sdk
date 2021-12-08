package com.exponea.sdk.view

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import com.exponea.sdk.models.InAppMessageButtonType
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.models.InAppMessagePayloadButton

internal class InAppMessageAlert : InAppMessageView {
    val dialog: AlertDialog
    override val isPresented: Boolean
        get() = dialog.isShowing

    constructor(
        context: Context,
        payload: InAppMessagePayload,
        onButtonClick: (InAppMessagePayloadButton) -> Unit,
        onDismiss: () -> Unit
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(payload.title)
        builder.setMessage(payload.bodyText)
        if (payload.buttons != null) {
            for (button in payload.buttons) {
                if (button.buttonType == InAppMessageButtonType.CANCEL) {
                    builder.setNegativeButton(button.buttonText) { _, _ -> dismiss() }
                } else {
                    builder.setPositiveButton(button.buttonText) { _, _ -> onButtonClick(button) }
                }
            }
        }
        // according to docs, dismissListener covers all dismiss options, cancelListener "most" of them.
        if (Build.VERSION.SDK_INT >= 17) {
            builder.setOnDismissListener { onDismiss() }
        } else {
            builder.setOnCancelListener { onDismiss() }
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

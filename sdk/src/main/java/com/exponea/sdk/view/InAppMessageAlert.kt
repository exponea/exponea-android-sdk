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
        payload.buttons?.forEach {
            if (it.buttonType == InAppMessageButtonType.CANCEL) {
                builder.setNegativeButton(it.buttonText) { _, _ -> dismiss() }
            } else {
                builder.setPositiveButton(it.buttonText) { _, _ -> onButtonClick(it) }
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

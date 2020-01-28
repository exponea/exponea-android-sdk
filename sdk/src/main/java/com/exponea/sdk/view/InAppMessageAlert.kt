package com.exponea.sdk.view

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import com.exponea.sdk.models.InAppMessagePayload

internal class InAppMessageAlert : InAppMessageView {
    val dialog: AlertDialog

    constructor(
        context: Context,
        payload: InAppMessagePayload,
        onButtonClick: () -> Unit,
        onDismiss: () -> Unit
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(payload.title)
        builder.setMessage(payload.bodyText)
        builder.setNeutralButton(payload.buttonText) { _, _ -> onButtonClick() }
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

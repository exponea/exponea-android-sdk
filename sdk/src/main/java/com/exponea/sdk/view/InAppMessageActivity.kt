package com.exponea.sdk.view

import androidx.appcompat.app.AppCompatActivity
import com.exponea.sdk.Exponea

internal class InAppMessageActivity : AppCompatActivity() {

    private var presentedMessageView: InAppMessageView? = null

    override fun onResume() {
        super.onResume()

        val presenting = Exponea.presentedInAppMessage
        if (presenting == null) {
            finish()
            return
        }
        val inAppMessageView = InAppMessagePresenter.getView(
            this,
            presenting.messageType,
            presenting.payload,
            presenting.image,
            presenting.timeout,
            { button ->
                presenting.actionCallback(button)
                finish()
            },
            {
                presenting.dismissedCallback()
                finish()
            }
        )
        if (inAppMessageView != null) {
            inAppMessageView.show()
            presentedMessageView = inAppMessageView
        } else {
            presenting.failedCallback()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val messageViewToDestroy = presentedMessageView ?: return
        messageViewToDestroy.dismiss()
        presentedMessageView = null
    }
}

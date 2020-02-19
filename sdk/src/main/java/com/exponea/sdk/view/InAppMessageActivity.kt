package com.exponea.sdk.view

import androidx.appcompat.app.AppCompatActivity
import com.exponea.sdk.Exponea

internal class InAppMessageActivity : AppCompatActivity() {
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
        } else {
            finish()
        }
    }
}

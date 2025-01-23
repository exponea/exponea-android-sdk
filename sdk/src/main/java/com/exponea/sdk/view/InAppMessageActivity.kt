package com.exponea.sdk.view

import androidx.appcompat.app.AppCompatActivity
import com.exponea.sdk.Exponea

internal class InAppMessageActivity : AppCompatActivity() {

    internal var presentedMessageView: InAppMessageView? = null

    override fun onResume() {
        super.onResume()

        presentedMessageView = Exponea.getPresentedInAppMessageView(this)

        if (presentedMessageView == null) {
            finish()
        } else {
            presentedMessageView!!.show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val messageViewToDestroy = presentedMessageView ?: return
        messageViewToDestroy.dismiss()
        presentedMessageView = null
    }
}

package com.exponea.sdk.view

import com.exponea.sdk.services.OnIntegrationStoppedCallback

internal interface InAppMessageView : OnIntegrationStoppedCallback {
    val isPresented: Boolean
    fun show()
    fun dismiss()
    override fun onIntegrationStopped()
}

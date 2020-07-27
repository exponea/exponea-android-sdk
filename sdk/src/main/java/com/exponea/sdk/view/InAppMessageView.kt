package com.exponea.sdk.view

internal interface InAppMessageView {
    val isPresented: Boolean
    fun show()
    fun dismiss()
}

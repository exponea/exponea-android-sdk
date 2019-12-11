package com.exponea.sdk.manager

import com.exponea.sdk.models.InAppMessage

internal interface InAppMessageManager {
    fun preload(callback: ((Result<Unit>) -> Unit)? = null)
    fun get(): InAppMessage?
}

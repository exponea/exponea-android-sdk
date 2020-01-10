package com.exponea.sdk.repository

import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.InAppMessageDisplayState
import java.util.Date

internal interface InAppMessageDisplayStateRepository {
    fun get(message: InAppMessage): InAppMessageDisplayState
    fun setDisplayed(message: InAppMessage, date: Date)
    fun setInteracted(message: InAppMessage, date: Date)
    fun clear()
}

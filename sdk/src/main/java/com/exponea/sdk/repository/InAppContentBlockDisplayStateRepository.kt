package com.exponea.sdk.repository

import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockDisplayState
import java.util.Date

internal interface InAppContentBlockDisplayStateRepository {
    fun get(message: InAppContentBlock): InAppContentBlockDisplayState
    fun setDisplayed(message: InAppContentBlock, date: Date)
    fun setInteracted(message: InAppContentBlock, date: Date)
    fun clear()
}

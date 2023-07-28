package com.exponea.sdk.services.inappcontentblock

import com.exponea.sdk.models.InAppContentBlock

interface InAppContentBlockDataLoader {
    fun loadContent(placeholderId: String): InAppContentBlock?
}

package com.exponea.sdk.models

class MessageItemContent(
    var imageUrl: String? = null,
    var title: String? = null,
    var message: String? = null,
    var consentCategoryTracking: String? = null,
    var hasTrackingConsent: Boolean = true,
    var trackingData: Map<String, Any?> = emptyMap(),
    var actions: List<MessageItemAction> = emptyList(),
    var action: MessageItemAction? = null,
    var html: String? = null
)

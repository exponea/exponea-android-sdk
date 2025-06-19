package com.exponea.sdk.telemetry.model

enum class TelemetryEvent(val value: String) {
    SDK_CONFIGURE("sdkConfigure"),
    IDENTIFY_CUSTOMER("identifyCustomer"),
    ANONYMIZE("anonymize"),
    IN_APP_MESSAGE_FETCH("inappMessageFetch"),
    IN_APP_MESSAGE_SHOWN("inappMessageShown"),
    APP_INBOX_INIT_FETCH("appInboxInitFetch"),
    APP_INBOX_SYNC_FETCH("appInboxSyncFetch"),
    APP_INBOX_MESSAGE_SHOWN("appInboxMessageShown"),
    PUSH_NOTIFICATION_DELIVERED("pushNotificationDelivered"),
    PUSH_NOTIFICATION_SHOWN("pushNotificationShown"),
    CONTENT_BLOCK_INIT_FETCH("contentBlockInitFetch"),
    CONTENT_BLOCK_PERSONALISED_FETCH("inappContentBlockPersonalisedFetch"),
    CONTENT_BLOCK_SHOWN("inappContentBlockShown"),
    RTS_CALLBACK_REGISTERED("callbackRegistered"),
    RTS_GET_SEGMENTS("getSegments"),
    INTEGRATION_STOPPED("integrationStopped"),
    LOCAL_CUSTOMER_DATA_CLEARED("localCustomerDataCleared"),
    RECOMMENDATIONS_FETCHED("recommendationsFetched"),
    CONSENTS_FETCHED("concentsGot"),
    SELF_CHECK("pushNotificationsSelfCheck"),
    EVENT_COUNT("notFlushedEventsCount")
}

package com.exponea.sdk.telemetry.model

enum class EventType(val value: String) {
    INIT("init"),
    FETCH_RECOMMENDATION("fetchRecommendation"),
    FETCH_CONSENTS("fetchConsents"),
    SHOW_IN_APP_MESSAGE("showInAppMessage"),
    SELF_CHECK("selfCheck")
}

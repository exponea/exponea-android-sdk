package com.exponea.sdk.models

enum class EventType {
    /// Install event is fired only once when the app is first installed.
     INSTALL,

    /// Session start event used to mark the start of a session, typically when an app comes to foreground.
     SESSION_START,

    /// Session end event used to mark the end of a session, typically when an app goes to background.
    SESSION_END,

    /// Custom event tracking, used to report any custom events that you want.
    TRACK_EVENT,

    /// Tracking of customers is used to identify a current customer by some identifier.
    TRACK_CUSTOMER,

    /// Virtual and hard payments can be tracked to better measure conversions for example.
    PAYMENT,

    /// Event used for registering the push notifications token of the device with Exponea.
    PUSH_TOKEN,

    // For tracking that push notification has been delivered
    PUSH_DELIVERED,

    /// For tracking that a push notification has been opened.
    PUSH_OPENED,
}
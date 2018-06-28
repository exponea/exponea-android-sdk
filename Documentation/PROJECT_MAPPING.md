## 🌋 Project Mapping

Exponea SDK has the ability to track multiple project tokens for specifics event type to your project.

We provided a configuration where you can inform what event types should be identifyed by a list of project tokens.

Eg:

If you want to identify the project tokens `TOKEN_001` and `TOKEN_002` for the event type `TRACK_CUSTOMER` you should configure the `projectTokenRouteMap` in the configuration object as:

```
var projectTokenRouteMap = hashMapOf<EventType, MutableList<String>> (
        Pair(EventType.TRACK_CUSTOMER, mutableListOf("TOKEN_001", "TOKEN_002"))
)
```

When the Exponea SDK will flush this event to the Exponea API, the event will be fired twice with the same parameters, just changing the project token, which means that you will see the same event in the project `TOKEN_001` and `TOKEN_002`.

You can map as many projects tokens you want for the specifics event types:

```
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

    // For tracking that a push notification has been delivered
    PUSH_DELIVERED,

    /// For tracking that a push notification has been opened.
    PUSH_OPENED,
}
```

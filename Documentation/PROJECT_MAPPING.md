## 🌋 Project Mapping

Exponea SDK has the ability to track events of specified event types into multiple projects.

Configuration contains a map where you can specify event types and projects into which you'd like to track events. A project is identified by it's baseUrl, project token and authorization token.

Events are always tracked into default project and to all projects that you specify in the mapping.

Eg:

If you want to track push notification opened events to projects `project-a` and `project-b` you should configure the `projectRouteMap` in the configuration object as:

``` kotlin
val configuration = ExponeaConfiguration(
    baseURL = "https://api.exponea.com",
    projectToken = "default-project",
    authorization = "Token some-token",
    projectRouteMap = mapOf(
        EventType.PUSH_OPENED to listOf(
            ExponeaProject("https://api.exponea.com", "project-a", "Token token-a"),
            ExponeaProject("https://api.exponea.com", "project-b", "Token token-b")
        )
    )
)
```

When push notification is opened, Exponea SDK will track the event three times with the same parameters, just changing the project. Which means that you will see the same event in the projects `default-project`, `project-a` and `project-b`.

Project mapping can be used for these specific event types:

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

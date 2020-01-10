## 🔍 Configuration

The configuration object must be configured before starting using the SDK.

It's possible to initialize the configuration through a ExponeaConfiguration object or providing a configuration file with the same structure (keys). In case of initializing the SDK with the configuration file, it must be located at the assets folder of the application with the name `exponea_configuration.json`. An configuration file example can be seen [here](
../app/src/main/assets/exponea_configuration.json).

### Configuration Class
```
data class ExponeaConfiguration(
  // Default project token.
  var projectToken: String = "",
  // Map event types and project tokens to be send to Exponea API.
  var projectTokenRouteMap: HashMap<EventType, MutableList<String>> = hashMapOf(),
  // Authorization http header.
  var authorization: String? = null,
  // Base url for http requests to Exponea API.
  var baseURL: String = Constants.Repository.baseURL,
  // Content type value to make http requests.
  var contentType: String = Constants.Repository.contentType,
  // Maximum retries value to flush data to api.
  var maxTries: Int = 10,
  // Timeout session value considered for app usage.
  var sessionTimeout: Double = 20.0,
  // Flag to control automatic session tracking
  var automaticSessionTracking: Boolean = true,
  // Flag to control if the App will handle push notifications automatically.
  var automaticPushNotification: Boolean = true,
  // Icon to be showed in push notifications.
  var pushIcon: Int? = null,
  // Channel name for push notifications. Only for API level 26+.
  var pushChannelName: String = "Exponea",
  // Channel description for push notifications. Only for API level 26+.
  var pushChannelDescription: String = "Notifications",
  // Channel ID for push notifications. Only for API level 26+.
  var pushChannelId: String = "0",
  // Notification importance for the notification channel. Only for API level 26+.
  var pushNotificationImportance: Int = NotificationManager.IMPORTANCE_DEFAULT,
  /** A list of properties to be added to all tracking events */
  var defaultProperties: HashMap<String, Any> = hashMapOf(),
  /** How ofter the token is tracked */
  var tokenTrackFrequency: TokenFrequency = TokenFrequency.ON_TOKEN_CHANGE
)
```
#### projectToken

* Is your project token which can be found in the Exponea APP ```Project``` -> ```Overview```

#### projectTokenRouteMap

* In case you have more than one project token to track for one event, you should provide which "Routes" (tracking events) each project token should be track.

Eg:

```
var projectTokenRouteMap = hashMapOf<EventType, MutableList<String>> (
        Pair(EventType.TRACK_CUSTOMER, mutableListOf("ProjectTokenA", "ProjectTokenB")),
        Pair(EventType.TRACK_EVENT, mutableListOf("ProjectTokenA", "ProjectTokenC"))
)
```

For detailed information, please go to [Project Mapping documentation](../Documentation/PROJECT_MAPPING.md)

#### authorization

* Supported by token authentication.
* For more information, please click [here](https://docs.exponea.com/v2/reference#token-authentication)

#### baseURL

* If you have you custom base URL, you can set up this property.
* Default value `https://api.exponea.com`

#### contentType

* Content type value to make http requests.
* Default value `application/json`

#### maxTries

* Maximum number of retries to flush data to Exponea API.
* SDK will consider the value to be flushed if this number is exceed and delete from the queue.

#### sessionTimeout

* Session is a real time spent in the App, it starts when the App is launched and ends when the App goes to background.
* This value will be used to calculate the session timing.
* Minimal timeout value is **1** second
* Maximal timeout value is **120** seconds
* Default timeout value: **20** seconds

#### automaticSessionTracking

* Flag to control the automatic tracking of user sessions.
* When set to true, the SDK will
automatically send `session_start` and `session_end` events to Exponea API

#### automaticPushNotification

* Controls if the SDK will handle push notifications automatically.

#### pushIcon

* Icon to be displayed when show a push notification.

#### pushChannelName

* Name of the Channel to be created for the push notifications.
* Only available for API level 26+. More info [here](https://developer.android.com/training/notify-user/channels)

#### pushChannelDescription

* Description of the Channel to be created for the push notifications.
* Only available for API level 26+. More info [here](https://developer.android.com/training/notify-user/channels)

#### pushChannelId

* Channel ID for push notifications.
* Only available for API level 26+. More info [here](https://developer.android.com/training/notify-user/channels)

#### pushNotificationImportance

* Notification importance for the notification channel.
* Only available for API level 26+. More info [here](https://developer.android.com/training/notify-user/channels)

#### defaultProperties

* The properties defined on this setting will always be sent with all triggered tracking events.

#### tokenTrackFrequency

* Indicates the frequency which the Firebase token should be tracked
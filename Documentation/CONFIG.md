## 🔍 Configuration

The configuration object must be configured before starting using the SDK.

> **NOTE:** Using of some API is allowed before SDK initialization in case that previous initialization process was done.
> These API methods are:
> - Exponea.handleCampaignIntent
> - Exponea.handleRemoteMessage
> - Exponea.handleNewToken
> - Exponea.handleNewHmsToken
> 
> In such a case, method will track events with configuration of last initialization. Please consider to do SDK initialization in `Application::onCreate` in case of update of your application to apply a fresh new configuration.

It's possible to initialize the configuration through a ExponeaConfiguration object or providing a configuration file with the same structure (keys). In case of initializing the SDK with the configuration file, it must be located at the assets folder of the application with the name `exponea_configuration.json`. An configuration file example can be seen [here](
../app/src/main/assets/exponea_configuration.json).

### Configuration Class
``` kotlin
data class ExponeaConfiguration(
  // Default project token.
  var projectToken: String = "",

  // Map event types and project tokens to be send to Exponea API.
  var projectRouteMap: Map<EventType, List<ExponeaProject>> = mapOf()

  // Authorization http header.
  var authorization: String? = null,

  // Base url for http requests to Exponea API.
  var baseURL: String = Constants.Repository.baseURL,

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

  // Accent color of push notification icon and buttons
  var pushAccentColor: Int? = null,

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
  
  /** If true, default properties are applied also for 'identifyCustomer' event. */
  var allowDefaultCustomerProperties: Boolean = true
  
  /** If true, Customer Token authentication is used */
  var advancedAuthEnabled: Boolean? = null
)
```
#### projectToken

* Is your project token which can be found in the Exponea APP ```Project``` -> ```Overview```
* If you need to switch project settings during runtime of the application, you can use [Anonymize feature](./ANONYMIZE.md)

#### projectRouteMap

* In case you have more than one project to track for one event, you should provide which "Routes" (tracking events) each project token should be track.

Eg:

``` kotlin
var projectRouteMap = mapOf<EventType, List<ExponeaProject>> (
    EventType.TRACK_CUSTOMER to listOf(
        ExponeaProject(
            "https://api.exponea.com",
            "project-token",
            "Token authorization-token"
        )
    )
)
```

For detailed information, please go to [Project Mapping documentation](../Documentation/PROJECT_MAPPING.md)

#### authorization

* Supported by token authentication.
* Format `"Token <token>"` where `<token>` is an Exponea **public** key.
* For more information, please see [Exponea API documentation](https://docs.exponea.com/reference#access-keys)

#### baseURL

* If you have you custom base URL, you can set up this property.
* Default value `https://api.exponea.com`

#### maxTries

* Maximum number of retries to flush data to Exponea API.
* SDK will consider the value to be flushed if this number is exceed and delete from the queue.

#### sessionTimeout

* Session is a real time spent in the App, it starts when the App is launched and ends when the App goes to background.
* This value will be used to calculate the session timing.
* Default timeout value: **20** seconds

#### automaticSessionTracking

* Flag to control the automatic tracking of user sessions.
* When set to true, the SDK will
automatically send `session_start` and `session_end` events to Exponea API

#### automaticPushNotification

* Controls if the SDK will handle push notifications automatically.

#### pushIcon

* Icon to be displayed when show a push notification.

#### pushAccentColor

* Accent color of push notification. Changes color of small icon and notification buttons. e.g. `Color.GREEN`
    > This is a color id, not a resource id. When using colors from resources you have to get the resource first: `context.resources.getColor(R.color.something)`

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

#### allowDefaultCustomerProperties

* Flag to apply `defaultProperties` list to `identifyCustomer` tracking event
* Default value `true`

#### tokenTrackFrequency

* Indicates the frequency which the Firebase token should be tracked

#### advancedAuthEnabled

* If set, advanced authorization is used for communication with BE for API listed in [JWT Authorization](./AUTHORIZATION.md)
* For more info see [authorization setup](./AUTHORIZATION.md)

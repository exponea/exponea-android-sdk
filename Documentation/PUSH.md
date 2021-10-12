
## 📣  Push Notifications

Exponea allows you to easily create complex scenarios which you can use to send push notifications directly to your customers. The following section explains how to integrate push notifications into your app.

- [Standard (Firebase) integration](#standard-firebase-integration)
- [Huawei integration](#huawei-integration)


## Standard (Firebase) integration

For firebase push notifications to work, you'll need to setup a few things:
- create a Firebase project
- integrate Firebase into your application 
- set the Firebase server key in the Exponea web app
- add and register firebase messaging service
- add a broadcast listener for opening push notifications

We've created a [Firebase quick start guide](../Guides/PUSH_QUICKSTART_FIREBASE.md) that will guide you through these steps.

## Huawei integration
For huawei push notifications to work, you'll need to setup a few things:
- register for a Huawei developer account
- create a new Huawei app
- enter Huawei credentials in the Exponea web app
- integrate the Huawei messaging SDK into your app
- add and register huawei messaging service
- add a broadcast listener for opening push notifications

We've created a [Huawei quick start guide](../Guides/PUSH_QUICKSTART_HUAWEI.md) that will guide you through these steps.

## Automatic tracking of Push Notifications

In the [Exponea SDK configuration](CONFIG.md), you can enable or disable the automatic push notification tracking by setting the Boolean value to the `automaticPushNotification` property and potentially setting up the desired frequency to the `tokenTrackFrequency`(default value is ON_TOKEN_CHANGE).

With `automaticPushNotification` enabled, the SDK will correctly display push notifications from Exponea and track a "campaign" event for every delivered/opened push notification with the correct properties.

## Responding to Push notifications

When creating notification using Exponea Web App, you can choose from 3 different actions to be used when tapping the notification or additional buttons on notification.

### 1. Open app
Open app action generates an intent with action `com.exponea.sdk.action.PUSH_CLICKED`. To respond to it, you need to setup a BroadcastReceiver in your Android manifest.

``` xml
<receiver
    android:name="MyReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="com.exponea.sdk.action.PUSH_CLICKED" />
    </intent-filter>
</receiver>
```

In the BroadcastReceiver you can launch a corresponding activity(e.g. your main activity). Campaign data is included in the intent as `ExponeaPushReceiver.EXTRA_DATA`.
``` kotlin
class MyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Extract payload data
        val data = intent.getParcelableExtra<NotificationData>(
          ExponeaPushReceiver.EXTRA_DATA
        )
        // Process the data if you need to

        // Start an activity
        val launchIntent = Intent(context, MainActivity::class.java)
        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(context, launchIntent, null)
    }
}
```

### 2. Deep link
Deep link action creates "view" intent that contains the url specified when setting up this action. To respond to this intent, create intent filter on the activity that should handle it in your Android manifest file. More information can be found in the [official Android documentation](https://developer.android.com/training/app-links/deep-linking).
``` xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />

    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />

    <!-- Accepts URIs that begin with "my-schema://”-->
    <data android:scheme="my-schema" />
</intent-filter>
```

### 3. Open web browser
Open web browser is handled automatically by the SDK and no work is required from the developer to handle it.

## Handling notification payload extra data
  You can setup notifications to contain extra data payload. Whenever a notification arrives the Exponea SDK will call `notificationCallback` that you can set on the `Exponea` object. The extras are a `Map<String, String>`.

#### 💻 Usage

``` kotlin
Exponea.notificationDataCallback = {
     extra -> //handle the extras value
}
```

Note that if a previous data was received and no listener was attached to the callback, that data will be dispatched as soon as a listener is attached.

> If your app is not running in the background, the SDK will auto-initialize when push notification is received. In this case, `Exponea.notificationDataCallback` is not set, so the callback will be called after you attach the listener(next app start). If you need to respond to the notification received immediately, implement your own `FirebaseMessagingService` and set the notification data callback in `onMessageReceived` function before calling `Exponea.handleRemoteMessage`. 

## Silent push notifications
Exponea web app allows you to setup silent push notifications, that are not displayed to the user. The SDK tracks `campaign` event when the push notification is delivered, just like for regular notifications. There is no opening for those notifications, but if you have set up extra data in the payload, the SDK will call `Exponea.notificationDataCallback` as described in [Handling notification payload extra data](#Handling-notification-payload-extra-data).

## Manual tracking of Push Notifications
In case you decide to deactivate the automatic push notification, or wish to track push notifications from other providers, you can still track events manually.

#### Track Push Token (FCM)

``` kotlin
fun trackPushToken(
        token: String
)
```
#### 💻 Usage

``` kotlin
Exponea.trackPushToken(
        token = "382d4221-3441-44b7-a676-3eb5f515157f"
)
```

#### Track Delivered Push Notification

``` kotlin
fun trackDeliveredPush(
        data: NotificationData? = null,
        timestamp: Double? = null
)
```

#### 💻 Usage

``` kotlin
// create NotificationData from your push payload
val notificationData = NotificationData(
    subject = "my subject",
    campaignName = "My campaign",
    actionName = "my-action",
    platform = "android",
    ...
)
Exponea.trackDeliveredPush(
        data = notificationData
        timestamp = currentTimeSeconds()
)
```

#### Track Clicked Push Notification

``` kotlin
fun trackClickedPush(
        data: NotificationData? = null,
        timestamp: Double? = null
)
```

#### 💻 Usage

``` kotlin
// create NotificationData from your push payload
val notificationData = NotificationData(
    subject = "my subject",
    campaignName = "My campaign",
    actionName = "my-action",
    platform = "android",
    ...
)
Exponea.trackClickedPush(
        data = notificationData
        timestamp = currentTimeSeconds()
)
```

## Troubleshooting
In case of push notifications not working for you, these are frequent issues with the most likely solutions.

### Push notifications do not open the app after clicking on the notification on Xiaomi Redmi devices

Xiaomi MIUI is handling battery optimization in its own way and can sometimes affect the behavior of push notifications. 
If battery optimization is on for devices with MIUI, it can make push notifications stop showing or not working after the click. Unfortunately, there is nothing we can do on our end to prevent this, but you can try this to solve the issues:

-   Turn off any battery optimizations in Settings->Battery & Performance you can
-   Set the "No restrictions" option in battery saver options for your app
-   And (probably) most important, turn off Memory and MIUI Optimization under Developer Options